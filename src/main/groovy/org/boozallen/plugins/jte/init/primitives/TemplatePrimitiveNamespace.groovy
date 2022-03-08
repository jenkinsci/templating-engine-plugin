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

/**
 * Stores a collection of TemplatePrimitives
 */
class TemplatePrimitiveNamespace implements Serializable {

    private static final long serialVersionUID = 1L
    String name
    List<TemplatePrimitive> primitives = []
    void add(TemplatePrimitive primitive){
        primitives.add(primitive)
    }

    TemplatePrimitiveNamespace parent = null

    TemplatePrimitiveNamespace getParent(){ return parent }
    void setParent(TemplatePrimitiveNamespace parent){ this.parent = parent }

    Object getProperty(String property){
        TemplatePrimitive primitive = primitives.find{ p -> p.getName() == property }
        if(primitive){
            return primitive.getValue(null, true)
        }
        throw new JTEException("Primitive ${property} not found in ${name}")
    }

    Object methodMissing(String methodName, Object args){
        TemplatePrimitive primitive = primitives.find{ p -> p.getName() == methodName }
        if(primitive){
            Object value = primitive.getValue(null, true)
            return args.size() > 0 ? value.call(*args) : value.call()
        }
        throw new JTEException("Primitive ${methodName} not found in ${name}")
    }

}
