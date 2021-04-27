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
package org.boozallen.plugins.jte.init.primitives.injectors

import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.jenkinsci.plugins.workflow.cps.CpsScript

/**
 * JTE primitive that represents a pre-populated variable
 */
class Keyword extends TemplatePrimitive{

    private static final long serialVersionUID = 1L
    String name
    Object value

    @Override String getName(){ return name }
    @Override String toString(){ return "Keyword '${name}'" }
    @SuppressWarnings("UnusedMethodParameter")
    Object getValue(CpsScript script, Boolean skipOverloaded = false){
        if(! skipOverloaded){
            isOverloaded()
        }
        return value
    }

}
