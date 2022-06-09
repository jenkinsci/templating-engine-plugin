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

import hudson.Extension
import hudson.FilePath
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.jenkinsci.plugins.workflow.cps.DSL
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import java.lang.reflect.Field

/**
 * Produces StepWrappers
 */
@SuppressWarnings(['NoDef', 'MethodReturnTypeRequired'])
class StepWrapperFactory{

    private final CpsFlowExecution exec

    StepWrapperFactory(CpsFlowExecution exec){
        this.exec = exec
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
        StepContext stepContext = new StepContext(library: library, name: name, isAlias: false)
        StepWrapper step = new StepWrapper(
            stepContext: stepContext,
            config: config,
            sourceFile: filePath.absolutize().getRemote(),
            isLibraryStep: true
        )
        StepWrapperScript script = prepareScript(step, sourceText)
        step.setScript(script)
        return step
    }

    /**
     * Creates an instance of the default step implementation
     *
     * @param name
     * @param stepConfig
     * @return a StepWrapper instance
     */
    StepWrapper createDefaultStep(String name, Map stepConfig){
        // get the source text for the default step implementation
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String defaultStep = uberClassLoader.loadClass(self).getResource("defaultStepImplementation.groovy").text

        /*
         * this feature was never documented and would prefer to remove it.
         * not sure if people read the source code and rely on this
         * so logging a warning if it's in use that it will be removed
         * in a future release
         */
        if(stepConfig.containsKey("name")){
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printWarning("Overriding the name of a default step implementation is deprecated and will be removed in a future release.")
        }
        stepConfig.name = stepConfig.name ?: name
        StepContext stepContext = new StepContext(library: null, name: name, isAlias: false)
        StepWrapper step = new StepWrapper(
            stepContext: stepContext,
            config: stepConfig,
            sourceText: defaultStep,
            isDefaultStep: true
        )

        StepWrapperScript script = prepareScript(step, defaultStep)
        step.setScript(script)
        return step
    }

    /**
     * Produces a no-op StepWrapper
     * @param stepName the name of the step to be created
     * @return a no-op StepWrapper
     */
    StepWrapper createNullStep(String stepName){
        String nullStep = "def call(Object[] args){ println \"Step ${stepName} is not implemented.\" }"
        LinkedHashMap config = [:]
        StepContext stepContext = new StepContext(library: null, name: stepName, isAlias: false)
        StepWrapper step = new StepWrapper(
            stepContext: stepContext,
            config: config,
            sourceText: nullStep,
            isTemplateStep: true
        )
        StepWrapperScript script = prepareScript(step, nullStep)
        step.setScript(script)
        return step
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
    StepWrapperScript prepareScript(StepWrapper step, String sourceText){
        StepWrapperScript script
        /*
         * parse the step the same way Jenkins parses a Jenkinsfile
         * this is easiest way to appropriately attach the flowOwner
         * of the template to the Step. attaching the flowOwner is
         * necessary for certain Jenkins Pipeline steps to work appropriately.
         */
        if(exec.getShell() == null || exec.getTrustedShell() == null){
            exec.parseScript()
        }
        String modifiedSource = """
        @groovy.transform.BaseScript ${StepWrapperScript.getName()} _
        ${sourceText}
        """
        GroovyShell shell = exec.getTrustedShell()
        String scriptName = "JTE_${step.library ?: "Default"}_${step.name}"
        try {
            try {
                script = shell.reparse(scriptName, modifiedSource) as StepWrapperScript
            } catch (LinkageError e) {
                script = shell.getClassLoader().loadClass(scriptName).newInstance()
            }
        } catch(any){
            TemplateLogger logger = new TemplateLogger(exec.getOwner().getListener())
            logger.printError("Failed to parse step text. Library: ${step.library}. Step: ${step.name}.")
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
        TemplateBinding binding = new TemplateBinding()
        binding.setVariable("steps", new DSL(exec.getOwner()))
        script.with{
            setBinding(binding)
            setConfig(step.config)
            setBuildRootDir(exec.getOwner().getRootDir())
            setResourcesPath("jte/${step.library}/resources")
        }
        return script
    }

    /**
     * Registers a compiler customization for parsing StepWrappers
     */
    @Extension
    static class StepWrapperShellDecorator extends GroovyShellDecorator {

        GroovyShellDecorator forTrusted() {
            return new InnerShellDecorator()
        }

        class InnerShellDecorator extends GroovyShellDecorator {

            /**
             * Automagically adds imports to library step files
             * @param execution
             * @param ic
             */
            @Override
            void customizeImports(CpsFlowExecution execution, ImportCustomizer ic){
                ic.addStarImports("org.boozallen.plugins.jte.init.primitives.hooks")
                ic.addImport(StepAlias.getName())
            }

            @Override
            void configureShell(CpsFlowExecution exec, GroovyShell shell) {
                if(exec == null){
                    return
                }
                FlowExecutionOwner flowOwner = exec.getOwner()
                WorkflowRun run = flowOwner.run()
                WorkflowJob job = run.getParent()
                if(job.getDefinition() instanceof TemplateFlowDefinition) {
                    ClassLoader common = getCommonClassLoader(run, shell)
                    Field f = GroovyShell.getDeclaredField("loader")
                    f.setAccessible(true)
                    f.set(shell, common)
                }
            }

            ClassLoader getCommonClassLoader(WorkflowRun run, GroovyShell shell) {
                TemplatePrimitiveCollector jte = run.getAction(TemplatePrimitiveCollector)
                if (jte == null) {
                    jte = new TemplatePrimitiveCollector()
                }
                if (jte.loader == null) {
                    jte.loader = shell.getClassLoader()
                    run.addOrReplaceAction(jte)
                }
                File rootDir = run.getRootDir()
                File srcDir = new File(rootDir, "jte/src")
                if (srcDir.exists()){
                    jte.loader.addURL(srcDir.toURI().toURL())
                }
                return jte.loader
            }

        }
    }

}
