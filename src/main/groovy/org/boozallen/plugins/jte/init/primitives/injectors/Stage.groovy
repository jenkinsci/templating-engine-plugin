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

import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.jenkinsci.plugins.workflow.cps.CpsScript

/**
 *  represents a group of library steps to be called.
 */

@SuppressWarnings("NoDef")
class Stage extends TemplatePrimitive{

    private static final long serialVersionUID = 1L
    String name
    ArrayList<String> steps

    Stage(){}

    Stage(String name, ArrayList<String> steps){
        this.name = name
        this.steps = steps
    }

    @Override String getName(){ return name }

    @Override String toString(){
        return "Stage '${name}'"
    }

    @SuppressWarnings("UnusedMethodParameter")
    Object getValue(CpsScript script, Boolean skipOverloaded = false){
        if(! skipOverloaded){
            isOverloaded()
        }
        return getCPSClass().newInstance(parent: this)
    }

    private Class getCPSClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("StageCPS.groovy").text
        return TemplatePrimitiveInjector.parseClass(classText)
    }

}
