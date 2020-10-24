/*
    Copyright 2018 Booz Allen Hamilton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.boozallen.plugins.jte.init.primitives

import hudson.Extension
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger

/**
 * Stores a run's TemplatePrimitives on individual namespaces for each type of primitive
 */
class TemplateBindingRegistry implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * The name used to reference the namespace collector from the pipeline
     */
    static final String VARIABLE_NAME = "jte"

    String getVariableName(){ return VARIABLE_NAME }

    @Extension static class JTE extends ReservedVariableName{
        @Override String getName(){ return TemplateBindingRegistry.VARIABLE_NAME }
        @Override String getExceptionMessage(){
            return "The variable name ${VARIABLE_NAME} is reserved by JTE for the template primitive namespace"
        }
    }

    List<PrimitiveNamespace> namespaces = []

    /**
     * Allows users to access PrimitiveNamespaces via their self-defined identifiers
     * @param property
     * @return
     */
    Object getProperty(String property){
        PrimitiveNamespace namespace = namespaces.find{ n ->
            n.getName() == property
        }
        if(!namespace){
            throw new JTEException("JTE does not have a primitive namespace for ${property}")
        }
        return namespace
    }

    void add(TemplatePrimitive primitive){
        // get the injector that created this primitive
        Class<? extends TemplatePrimitiveInjector> injector = primitive.getInjector()

        String namespaceKey = injector.getNamespaceKey()

        // check if this type of namespace already exists
        PrimitiveNamespace namespace = namespaces.find{ n ->
            n.getName() == namespaceKey
        }

        // if this namespace does NOT exist - create it.
        if(!namespace){
            namespace = injector.createNamespace()
            namespaces.push(namespace)
        }
        // add the primitive to the namespace
        namespace.add(primitive)
    }

    List<String> getPrimitivesByName(String primitiveName){
        List names = []
        namespaces.each{ n ->
            Object name = n.getPrimitivesByName(primitiveName)
            name && names.push(name)
        }
        return names.flatten().collect{ n -> "${VARIABLE_NAME}.${n}" }
    }

    /**
     * @return the list of variable names corresponding to TemplatePrimitives
     */
    Set<String> getVariables(){
        return namespaces*.getVariables().flatten() as Set<String>
    }

    /**
     * needed for using as target of 'in' keyword; 'contains' overloading did not implement 'in'
     * @param o
     * @return
     */
    boolean isCase(Object o){
        return o in getVariables()
    }

    void printAllPrimitives(TemplateLogger logger){
        namespaces.each{ namespace ->
            namespace.printAllPrimitives(logger)
        }
    }

}
