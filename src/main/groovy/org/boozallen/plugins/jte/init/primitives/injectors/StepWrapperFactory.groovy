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
import jenkins.scm.api.SCMFile
import org.boozallen.plugins.jte.util.TemplateScriptEngine

/**
 * static methods to produce a StepWrapper from various sources
 */
class StepWrapperFactory{
    static final String CONFIG_VAR = "config"

    /**
     * creates a StepWrapper from an SCMFile
     */
    static def createFromFile(SCMFile file, String library, Binding binding, Map libConfig){
        String name = file.getName() - ".groovy" 
        String stepText = file.contentAsString()
        return createFromString(stepText, binding, name, library, libConfig)
    }

    /**
     * creates a default step implementation StepWrapper
     */
    static def createDefaultStep(Binding binding, String name, Map stepConfig){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String defaultImpl = uberClassLoader.loadClass(self).getResource("defaultStepImplementation.groovy").text
        if (!stepConfig.name) stepConfig.name = name 
        return createFromString(defaultImpl, binding, name, "Default Step Implementation", stepConfig) 
    }

    /**
     * creates a NullStepWrapper
     */
    static def createNullStep(String stepName, Binding binding){
        String nullImpl = "def call(){ println \"Step ${stepName} is not implemented.\" }"
        return createFromString(nullImpl, binding, stepName, "Null Step", [:])
    }

    /**
     *  creates a StepWrapper from a string
     */
    static def createFromString(String stepText, Binding binding, String name, String library, Map libConfig){
        Class StepWrapper = getPrimitiveClass()
        Script impl = TemplateScriptEngine.parse(stepText, binding)
        impl.metaClass."get${CONFIG_VAR.capitalize()}" << { return libConfig }
        impl.metaClass.getStageContext = {->  [ name: null, args: [:] ]}
        return StepWrapper.newInstance(binding: binding, impl: impl, name: name, library: library) 
    }

    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("StepWrapper.groovy").text
        return TemplateScriptEngine.parseClass(classText)
    }
}