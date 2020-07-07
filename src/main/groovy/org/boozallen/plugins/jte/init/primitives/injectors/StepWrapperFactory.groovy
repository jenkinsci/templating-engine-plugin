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
import org.boozallen.plugins.jte.init.primitives.ReservedVariableName
import hudson.Extension
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsScript

/**
 * Produces StepWrappers from a variety of input sources
 *
 * @auther Steven Terrana
 */
class StepWrapperFactory{
    static final String CONFIG_VAR = "config"

    @Extension static class ReservedVariableNameImpl extends ReservedVariableName{
        static String getName(){ return CONFIG_VAR }
        static void throwPreLockException(){
            throw new Exception("Variable name ${CONFIG_VAR} is reserved for steps to access their library configuration")
        }
        static void throwPostLockException(){
            throw new Exception("Variable name ${CONFIG_VAR} is reserved for steps to access their library configuration")
        }
    }

    private FlowExecutionOwner flowOwner

    StepWrapperFactory(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
    }

    /**
     * Parses a step's text and produces an invocable Script
     *
     * @param scriptText the source code text of the step to be parsed
     * @param b the template binding
     * @return an invocable Script object representing the step
     */
    private Script parse(String scriptText, Binding b){
        CpsScript script = new CpsFlowExecution(
                scriptText,
                false,
                flowOwner,
                TemplateFlowDefinition.determineFlowDurabilityHint(flowOwner)
        ).parseScript()
        script.setBinding(b)
        return script
    }

    /**
     * Produces a StepWrapper
     *
     * @param stepText the source code text of the to-be step
     * @param binding the template binding
     * @param name the name of the step to be created
     * @param library the library that has contributed the step
     * @param libConfig the library configuration that will be resolvable via #CONFIG during execution
     * @return a StepWrapper representing the stepText
     */
    def createFromString(String stepText, Binding binding, String name, String library, Map libConfig){
        Class StepWrapper = getPrimitiveClass()
        Script impl = parse(stepText, binding)
        impl.metaClass."get${CONFIG_VAR.capitalize()}" << { return libConfig }
        impl.metaClass.getStageContext = {->  [ name: null, args: [:] ]}
        return StepWrapper.newInstance(binding: binding, impl: impl, name: name, library: library)
    }

    /**
     * Takes an SCMFile and produces a StepWrapper
     *
     * @param file the SCMFile containing the step's code
     * @param library the library contributing the step
     * @param binding the template binding
     * @param libConfig the library configuration that will be resolvable via #CONFIG during execution
     * @return a StepWrapper respresenting the step in SCMFile
     */
    def createFromFile(SCMFile file, String library, Binding binding, Map libConfig){
        String name = file.getName() - ".groovy" 
        String stepText = file.contentAsString()
        return createFromString(stepText, binding, name, library, libConfig)
    }

    /**
     * Creates an instance of the default step implementation
     *
     * @param the template binding
     * @param name
     * @param stepConfig
     * @return
     */
    def createDefaultStep(Binding binding, String name, Map stepConfig){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String defaultImpl = uberClassLoader.loadClass(self).getResource("defaultStepImplementation.groovy").text
        if (!stepConfig.name) stepConfig.name = name 
        return createFromString(defaultImpl, binding, name, "Default Step Implementation", stepConfig) 
    }

    /**
     * Produces a no-op StepWrapper
     * @param stepName the name of the step to be created
     * @param binding the template binding
     * @return a no-op StepWrapper
     */
    def createNullStep(String stepName, Binding binding){
        String nullImpl = "def call(){ println \"Step ${stepName} is not implemented.\" }"
        return createFromString(nullImpl, binding, stepName, null, [:])
    }

    /**
     * returns the CPS-transformed StepWrapper Class that will work during pipeline execution
     * @return the StepWrapper Class
     */
    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("StepWrapper.groovy").text
        return TemplateScriptEngine.parseClass(classText)
    }
}