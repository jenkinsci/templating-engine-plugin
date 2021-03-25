package org.boozallen.plugins.jte.init.primitives.injectors

import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.JTEException

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