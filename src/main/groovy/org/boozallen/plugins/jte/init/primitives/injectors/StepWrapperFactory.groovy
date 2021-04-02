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

import hudson.FilePath
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.util.TemplateLogger
import hudson.Extension
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.boozallen.plugins.jte.job.TemplateFlowDefinition

import javax.annotation.CheckForNull
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Produces StepWrappers
 */
@SuppressWarnings(['NoDef', 'MethodReturnTypeRequired'])
class StepWrapperFactory{

    /**
     * the variable to autowire to expose the library
     * configuration to the step
     */
    static final String CONFIG_VAR = "config"

    private final FlowExecutionOwner flowOwner

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
     * @param config the library configuration
     * @param optional {@link StageContext}
     * @param optional {@link HookContext}
     * @return an executable and wired executable script
     */
    @SuppressWarnings("ParameterCount")
    StepWrapperScript prepareScript(
            String library,
            String name,
            String source,
            LinkedHashMap config,
            StageContext stageContext = null,
            HookContext hookContext = null
    ){
        StepWrapperScript script
        /*
         * parse the step the same way Jenkins parses a Jenkinsfile
         *        this is easiest way to appropriately attach the flowOwner
         *        of the template to the Step. attaching the flowOwner is
         *        necessary for certain Jenkins Pipeline steps to work appropriately.
         */
        try{
            CpsFlowExecution exec = new CpsFlowExecution(
                source,
                false,
                flowOwner,
                TemplateFlowDefinition.determineFlowDurabilityHint(flowOwner)
            )
            // tell StepWrapperShellDecorator this is a step
            exec.metaClass[StepWrapperShellDecorator.FLAG] = true
            script = exec.parseScript() as StepWrapperScript
        } catch(any){
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printError("Failed to parse step text. Library: ${library}. Step: ${name}.")
            throw any
        }
        /*
         * set whatever runtime specific contexts are required for this step, such as:
         *
         * 1. our custom binding that prevents collisions
         * 2. the library configuration
         * 3. the base directory from which to fetch library resources
         * 4. an optional StageContext
         * 5. an optional HookContext
         */
        script.setBinding(new TemplateBinding())
        script.$initialize()
        script.setConfig(config)
        script.setBuildRootDir(flowOwner.getRootDir())
        script.setResourcesPath("jte/${library}/resources")
        stageContext && script.setStageContext(stageContext)
        hookContext  && script.setHookContext(hookContext)
        return script
    }

    /**
     * Registers a compiler customization for parsing StepWrappers
     */
    @Extension static class StepWrapperShellDecorator extends GroovyShellDecorator {

        private static final Logger LOGGER = new Logger(StepWrapperShellDecorator.name)
        /**
         * The name of a property that will be added to CpsFlowExecution's used to
         * parse a StepWrapper script.
         * <p>
         * Simplifies determining if a CpsFlowExecution's script compilation should
         * be modified by this decorator.
         */
        private static final String FLAG = "JTE_STEP"

        /**
         * Customizes th
         * @param execution the run's execution
         * @param cc the compiler configuration used to compile the step
         */
        @Override
        void configureCompiler(@CheckForNull final CpsFlowExecution execution, CompilerConfiguration cc) {
            if(execution.hasProperty(FLAG)){
                // auto import lifecycle hook annotations within steps
                ImportCustomizer ic = new ImportCustomizer()
                ic.addStarImports("org.boozallen.plugins.jte.init.primitives.hooks")
                cc.addCompilationCustomizers(ic)
                // set script base class to our own
                cc.setScriptBaseClass(StepWrapperScript.name)
            }
        }

        @Override
        void configureShell(@CheckForNull CpsFlowExecution execution, GroovyShell shell) {
            if(execution.hasProperty(FLAG)){
                FlowExecutionOwner owner = execution.getOwner()
                File jte = owner.getRootDir()
                File srcDir = new File(jte, "jte/src")
                if (srcDir.exists()){
                    if(srcDir.isDirectory()) {
                        shell.getClassLoader().addURL(srcDir.toURI().toURL())
                    } else {
                        LOGGER.log(Level.WARNING, "${srcDir.getPath()} is not a directory.")
                    }
                }
            }
        }
    }

    /**
     * takes a FilePath holding the source text for the step and
     * creates a StepWrapper instance
     *
     * @param filePath the FilePath where the source file can be found
     * @param library the library contributing the step
     * @param config the library configuration for the step
     * @return a StepWrapper instance
     */
    StepWrapper createFromFilePath(FilePath filePath, String library, Map config){
        String name = filePath.getBaseName()
        String sourceText = filePath.readToString()
        return new StepWrapper(
            name: name,
            library: library,
            config: config,
            sourceFile: filePath.absolutize().getRemote(),
            // parse to fail fast for step compilation issues
            script: prepareScript(library, name, sourceText, config),
            isLibraryStep: true
        )
    }

    /**
     * Creates an instance of the default step implementation
     *
     * @param name
     * @param stepConfig
     * @return a StepWrapper instance
     */
    StepWrapper createDefaultStep(String name, Map stepConfig){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String defaultStep = uberClassLoader.loadClass(self).getResource("defaultStepImplementation.groovy").text
        // will be nice to eventually use the ?= operator when groovy version gets upgraded
        stepConfig.name = stepConfig.name ?: name
        return new StepWrapper(
            name: name,
            library: null,
            config: stepConfig,
            sourceText: defaultStep,
            // parse to fail fast for step compilation issues
            script: prepareScript("Default Step Implementation", name, defaultStep, stepConfig),
            isDefaultStep: true
        )
    }

    /**
     * Produces a no-op StepWrapper
     * @param stepName the name of the step to be created
     * @return a no-op StepWrapper
     */
    StepWrapper createNullStep(String stepName){
        String nullStep = "def call(Object[] args){ println \"Step ${stepName} is not implemented.\" }"
        LinkedHashMap config = [:]
        return new StepWrapper(
            name: stepName,
            library: null,
            config: config,
            sourceText: nullStep,
            // parse to fail fast for step compilation issues
            script: prepareScript(null, stepName, nullStep, config),
            isTemplateStep: true
        )
    }

}
