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
package org.boozallen.plugins.jte.init

import hudson.Extension
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.hooks.CleanUp
import org.boozallen.plugins.jte.init.primitives.hooks.Init
import org.boozallen.plugins.jte.init.primitives.hooks.Notify
import org.boozallen.plugins.jte.init.primitives.hooks.Validate
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

import javax.annotation.CheckForNull
import java.lang.reflect.Field
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Decorates the pipeline template during compilation
 * <p>
 * Attaches the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding} to the pipeline template
 * prior to execution and modifies the template at compile time to invoke lifecycle hooks prior to and after
 * template execution
 *
 * @author Steven Terrana
 */
@Extension(ordinal=1.0D) // set ordinal > 0 so JTE comes before Declarative
class GroovyShellDecoratorImpl extends GroovyShellDecorator {

    private static final Logger LOGGER = Logger.getLogger(GroovyShellDecoratorImpl.name);

    /**
     * If the current pipeline run has a @see PipelineDecorator action then
     * fetch the already configured @see org.boozallen.plugins.jte.init.primitives.TemplateBinding
     * and ensure the parsed scripts have this binding during execution.
     */
    @Override
    void configureShell(@CheckForNull CpsFlowExecution context, GroovyShell shell) {
        if (!context) {
            return
        }
        FlowExecutionOwner owner = context.getOwner()
        WorkflowRun run = owner.run()
        PipelineDecorator pipelineDecorator = run.getAction(PipelineDecorator)
        if(pipelineDecorator){
            // attach the TemplateBinding
            TemplateBinding binding = pipelineDecorator.getBinding()
            Field shellBinding = GroovyShell.getDeclaredField("context")
            shellBinding.setAccessible(true)
            shellBinding.set(shell, binding)
            // add loaded libraries `src` directories to the classloader
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

    @Override
    GroovyShellDecorator forTrusted() {
        return this
    }

    /**
     * For all scripts, adds a star import for Lifecycle Hooks so that library steps need not
     * import them explicitly to use them.
     *
     * For the JTE pipeline template, customizes the compiler so that the template is wrapped
     * in a try-finally block so that the @Validation and @Init Lifecycle Hooks can be invoked
     * prior to template execution and @CleanUp and @Notify Lifecycle Hooks can be invoked post
     * template execution.
     */
    @Override
    void configureCompiler(@CheckForNull final CpsFlowExecution execution, CompilerConfiguration cc) {
        if(isFromJTE(execution)){
            cc.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.SEMANTIC_ANALYSIS) {

                @Override
                void call(SourceUnit source,
                          GeneratorContext context,
                          ClassNode classNode) throws CompilationFailedException {
                    if(!isTemplate(classNode)){
                        return
                    }
                    ModuleNode template = source.getAST()
                    List<Statement> statements = template.getStatementBlock().getStatements()

                    /*
                        TODO:
                        The references to Hook, Validate, Init, CleanUp, and Notify
                        are actually coming from HookInjector.
                        Need to come back to this and figure out how to use AstBuilder
                        to reference classes.  Right now, the AST getting produces is
                        trying to find those names as variables in the binding.
                        So i put them in the binding for now </shrug>
                    */
                    BlockStatement wrapper = (new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS){
                        try{
                            Hooks.invoke(Validate)
                            Hooks.invoke(Init)
                            // <-- this is where the template AST statements get injected
                        } finally{
                            Hooks.invoke(CleanUp)
                            Hooks.invoke(Notify)
                        }
                    }).first()

                    wrapper.getStatements().first().getTryStatement().addStatements(statements)
                    statements.clear()
                    statements.add(0, wrapper)
                }

                /*
                    need to ensure we're only modifying AST for the template.
                    an example of additional scripts getting compiled would be from a
                    template or library step that does something like:
                    evaluate("x = 1")

                    to understand why specifically checking for "WorkflowScript" see:
                    https://github.com/jenkinsci/workflow-cps-plugin/blob/workflow-cps-2.80/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsFlowExecution.java#L561

                    to understand where "evaluate" method comes from see:
                    https://github.com/jenkinsci/workflow-cps-plugin/blob/workflow-cps-2.80/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsScript.java#L178-L182
                 */
                boolean isTemplate(ClassNode classNode){
                    return classNode.getName() == "WorkflowScript"
                }
            })
        }
    }

    /**
     * determines if the current pipeline is using JTE
     */
    boolean isFromJTE(CpsFlowExecution execution){
        if(!execution){
            return false // no execution defined yet, still initializing
        }
        WorkflowJob job = execution.getOwner().run().getParent()
        FlowDefinition definition = job.getDefinition()
        return (definition in TemplateFlowDefinition)
    }

}
