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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.JTEException

/**
 * Stores loaded libraries' steps
 */
class LibraryNamespace extends TemplatePrimitiveNamespace{

    String name = LibraryStepInjector.KEY

    List<TemplatePrimitiveNamespace> libraries = []

    void add(TemplatePrimitiveNamespace library){
        libraries.add(library)
    }

    List<TemplatePrimitive> getPrimitives(){
        List<TemplatePrimitive> steps = []
        libraries.each{ library ->
            steps.addAll(library.getPrimitives())
        }
        return steps
    }

    Object getProperty(String property){
        TemplatePrimitiveNamespace library = libraries.find{ lib -> lib.getName() == property }
        if(library){
            return library
        }
        throw new JTEException("Library ${property} not found")
    }

}
