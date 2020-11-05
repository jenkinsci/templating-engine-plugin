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

import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger

/**
 * Subclasses of PrimitiveNamespace should overwrite getProperty to allow
 * the seamless interaction.
 *
 * For example, a PrimitiveNamespace identified by foo that holds a TemplatePrimitive
 * bar should be accessible via jte.foo.bar
 */
class PrimitiveNamespace implements Serializable{

    private static final long serialVersionUID = 1L
    private static final String TYPE_DISPLAY_NAME = "Primitive"

    /**
     * the key/name for this namespace
     */
    protected String name
    LinkedHashMap primitives = [:]
    String typeDisplayName = TYPE_DISPLAY_NAME

    TemplatePrimitiveInjector primitiveInjector

    /**
     * Add a new primitive to the namespace
     * @param primitive the primitive to be added
     */
    void add(TemplatePrimitive primitive){
        primitives[primitive.getName()] = primitive.getValue()
    }

    String getName(){
        return name
    }

    String getTypeDisplayName(){
        return typeDisplayName
    }

    String getMissingPropertyMessage(String name){
        return "${getTypeDisplayName()} ${name} not found"
    }

    LinkedHashMap getPrimitives(){
        return this.@primitives
    }

    Object getProperty(String name){
        MetaProperty meta = getClass().metaClass.getMetaProperty(name)
        if(meta){
            return meta.getProperty(this)
        }
        if(!getPrimitives().containsKey(name)){
            throw new JTEException(getMissingPropertyMessage(name))
        }
        return getPrimitives()[name]
    }

    /**
     * @return the variable names of the primitives in this namespace
     */
    Set<String> getVariables(){
        return this.primitives.keySet() as Set<String>
    }

    void printAllPrimitives(TemplateLogger logger){
        // default implementation
        logger.print( "created ${getTypeDisplayName()}s:\n" + getVariables().join("\n") )
    }

    Object getPrimitivesByName(String primitiveName){
        return primitives.containsKey(primitiveName) ? "${name}.${primitiveName}" : null
    }

}
