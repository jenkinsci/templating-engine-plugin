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

import hudson.AbortException
import hudson.FilePath
import jenkins.model.Jenkins
import jenkins.scm.api.SCMFile
import org.boozallen.plugins.jte.util.TemplateLogger
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
    /**
     * the variable to autowire to expose the library
     * configuration to the step
     */
    static final String CONFIG_VAR = "config"

    /**
     * reserves ${CONFIG_VAR} as a protected variable name in the TemplateBinding
     */
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
     *  Parses source code and turns it into a CPS transformed executable
     *  script that's been autowired appropriately for JTE.
     *
     * @param library the library contributing the step
     * @param name the name of the step
     * @param source the source code text
     * @param binding the TemplateBinding resolvable during invocation
     * @param config the library configuration
     * @return an executable and wired executable script
     */
    Script prepareScript(String library, String name, String source, Binding binding, Map config){
        CpsScript script
        try{
            script = new CpsFlowExecution(
                source,
                false,
                flowOwner,
                TemplateFlowDefinition.determineFlowDurabilityHint(flowOwner)
            ).parseScript()
        }catch(any){
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printError("Failed to parse step text. Library: ${library}. Step: ${name}.")
            throw any
        }

        script.metaClass."get${CONFIG_VAR.capitalize()}" << { return config }
        script.metaClass.getStageContext = {->  [ name: null, args: [:] ]}

        script.metaClass.resource = { String resource ->
            if(resource.startsWith("/")){
                throw new AbortException("JTE: The ${name} step from the ${library} library requested a resource '${resource}' that is not a relative path.  Must not begin with /.")
            }
            FilePath rootDir = new FilePath(flowOwner.getRootDir())
            FilePath resourceFile = rootDir.child("jte/${library}/resources/${resource}")
            if(!resourceFile.exists()){
                throw new AbortException("JTE: The ${name} step from the ${library} library requested a resource '${resource}' that does not exist")
            } else if(resourceFile.isDirectory()){
                throw new AbortException("JTE: The ${name} step from the ${library} library requested a resource '${resource}' that is a directory. Must be a file.")
            }
            return resourceFile.readToString()
        }

        script.setBinding(binding)

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
    def createFromString(String sourceText, Binding binding, String name, String library, Map config){
        Class StepWrapper = getPrimitiveClass()
        return StepWrapper.newInstance(
            name: name,
            library: library,
            config: config,
            sourceText: sourceText,
            // parse to fail fast for step compilation issues
            impl: prepareScript(library, name, sourceText, binding, config)
        )
    }

    /**
     * takes a FilePath holding the source text for the step and
     * creates a StepWrapper instance
     *
     * @param filePath the FilePath where the source file can be found
     * @param binding the TemplateBinding context to attach to the StepWrapper
     * @param library the library contributing the step
     * @param config the library configuration for the step
     * @return a StepWrapper instance
     */
    def createFromFilePath(FilePath filePath, Binding binding, String library, Map config){
        Class StepWrapper = getPrimitiveClass()
        String name = filePath.getBaseName()
        String sourceText = filePath.readToString()
        return StepWrapper.newInstance(
            name: name,
            library: library,
            config: config,
            sourceFile: filePath.absolutize().getRemote(),
            // parse to fail fast for step compilation issues
            impl: prepareScript(library, name, sourceText, binding, config)
        )
    }

    /**
     * Creates an instance of the default step implementation
     *
     * @param the template binding
     * @param name
     * @param stepConfig
     * @return a StepWrapper instance
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