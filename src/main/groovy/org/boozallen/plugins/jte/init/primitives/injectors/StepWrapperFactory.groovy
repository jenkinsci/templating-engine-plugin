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
    StepWrapper createFromFilePath(FilePath filePath, String library, Map config, Boolean isSandboxed){
        String name = filePath.getBaseName()
        String sourceText = filePath.readToString()
        StepContext stepContext = new StepContext(library: library, name: name, isAlias: false)
        StepWrapper step = new StepWrapper(
            stepContext: stepContext,
            config: config,
            sourceFile: filePath.absolutize().getRemote(),
            isLibraryStep: true,
            isSandboxed: isSandboxed
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
        if(exec.getShell() == null || exec.getTrustedShell() == null){
            exec.parseScript()
        }
        // this is equivalent to compilerConfiguration.setScriptBaseClass(StepWrapperScript)
        // but doesn't require a separate CompilerConfiugration between the pipeline template
        // and library steps - which would in turn require a different classloader, which
        // was the root cause of issue #279
        String modifiedSource = """
        ${sourceText}

        @groovy.transform.BaseScript ${StepWrapperScript.getName()} _
        """
        GroovyShell shell
        if (step.isSandboxed){
            shell = exec.getShell()
        } else {
            shell = exec.getTrustedShell()
        }
        String scriptName = "JTE_${step.library ?: "Default"}_${step.name}"
        try {
            try {
                script = shell.reparse(scriptName, modifiedSource) as StepWrapperScript
            } catch (LinkageError e) {
                // on restarts, we can run into a "class already define"
                // exception.. so just reload the script from the classloader.
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
        script.with{
            setBinding(new TemplateBinding())
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

        void runIfJTE(CpsFlowExecution exec, Closure body){
            if (exec == null) {
                return
            }
            FlowExecutionOwner flowOwner = exec.getOwner()
            WorkflowRun run = flowOwner.run()
            WorkflowJob job = run.getParent()
            if (job.getDefinition() instanceof TemplateFlowDefinition) {
                body.call()
            }
        }

        @Override
        void configureShell(CpsFlowExecution exec, GroovyShell shell){
            runIfJTE(exec) {
                // add JTE binding
                Field shellBinding = GroovyShell.getDeclaredField("context")
                shellBinding.setAccessible(true)
                shellBinding.set(shell, new TemplateBinding())
            }
        }

        @Override
        void customizeImports(CpsFlowExecution execution, ImportCustomizer ic) {
            ic.addStarImports("org.boozallen.plugins.jte.init.primitives.hooks")
            ic.addImport(StepAlias.getName())
        }

        GroovyShellDecorator forTrusted(){
            return new GroovyShellDecorator() {
                @Override
                void customizeImports(CpsFlowExecution execution, ImportCustomizer ic) {
                    ic.addStarImports("org.boozallen.plugins.jte.init.primitives.hooks")
                    ic.addImport(StepAlias.getName())
                }

                @Override
                void configureShell(CpsFlowExecution exec, GroovyShell shell) {
                    runIfJTE(exec){
                        ClassLoader common = getCommonClassLoader(exec.getOwner().run(), shell)
                        Field f = GroovyShell.getDeclaredField("loader")
                        f.setAccessible(true)
                        f.set(shell, common)
                    }
                }
            }
        }

        ClassLoader getCommonClassLoader(WorkflowRun run, GroovyShell shell) {
            TemplatePrimitiveCollector jte = run.getAction(TemplatePrimitiveCollector)
            // may be null at the beginning of the run
            if (jte == null) {
                jte = new TemplatePrimitiveCollector()
            }
            // loader would be null on pipeline restart because the field is transient
            if (jte.loader == null) {
                jte.loader = shell.getClassLoader()
                run.addOrReplaceAction(jte)
            }
            // add the JTE library classes to the common classloader
            // these would be added by the respective LibraryProvider's
            File rootDir = run.getRootDir()
            File srcDir = new File(rootDir, "jte/src")
            if (srcDir.exists()) {
                jte.loader.addURL(srcDir.toURI().toURL())
            }
            return jte.loader
        }

    }

}
