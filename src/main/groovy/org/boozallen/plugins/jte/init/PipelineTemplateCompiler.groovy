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
import org.boozallen.plugins.jte.init.primitives.hooks.HooksWrapper
import org.boozallen.plugins.jte.init.primitives.hooks.Init
import org.boozallen.plugins.jte.init.primitives.hooks.Notify
import org.boozallen.plugins.jte.init.primitives.hooks.Validate
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.codehaus.groovy.ast.ASTNode
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
import org.jenkinsci.plugins.workflow.job.WorkflowJob

import javax.annotation.CheckForNull
import java.lang.reflect.Field
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Decorates the pipeline template during compilation
 * <p>
 * modifies the template at compile time to invoke lifecycle hooks prior to and after template execution
 *
 * @author Steven Terrana
 */
@Extension(ordinal=1.0D) // set ordinal > 0 so JTE comes before Declarative
class PipelineTemplateCompiler extends GroovyShellDecorator {

    private static final Logger LOGGER = Logger.getLogger(PipelineTemplateCompiler.name);

    @Override
    void configureShell(@CheckForNull CpsFlowExecution context, GroovyShell shell) {
        if (!isFromJTE(context)) {
            return
        }

        // use our custom binding that checks for collisions
        Field shellBinding = GroovyShell.getDeclaredField("context")
        shellBinding.setAccessible(true)
        shellBinding.set(shell, new TemplateBinding())

        // add loaded libraries `src` directories to the classloader
        File jte = context.getOwner().getRootDir()
        File srcDir = new File(jte, "jte/src")
        if (srcDir.exists()){
            if(srcDir.isDirectory()) {
                shell.getClassLoader().addURL(srcDir.toURI().toURL())
            } else {
                LOGGER.log(Level.WARNING, "${srcDir.getPath()} is not a directory.")
            }
        }
    }

    @Override
    GroovyShellDecorator forTrusted() {
        return this
    }

    /**
     * For the JTE pipeline template, customizes the compiler so that the template is wrapped
     * in a try-finally block so that the @Validation and @Init Lifecycle Hooks can be invoked
     * prior to template execution and @CleanUp and @Notify Lifecycle Hooks can be invoked post
     * template execution.
     */
    @Override
    @SuppressWarnings("MethodSize")
    void configureCompiler(@CheckForNull final CpsFlowExecution execution, CompilerConfiguration cc) {
        if (isFromJTE(execution)) {
            cc.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {

                @Override
                void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                    if (!isTemplate(classNode)) {
                        return
                    }
                    // represents the AST of the user-provided template
                    ModuleNode template = source.getAST()
                    List<Statement> statements = template.getStatementBlock().getStatements()

                    /* We want to seamlessly wrap the template in code for hooks like:
                     *
                     *   _JTE_hookContext_exceptionThrown = false
                     *   try{
                     *     Hooks.invoke(Validation)
                     *     Hooks.invoke(Init)
                     *     --> Insert Template Code Here <--
                     *   } catch(Exception e){
                     *     _JTE_hookContext_exceptionThrown = true
                     *     throw e
                     *   } finally {
                     *     Hooks.invoke(CleanUp, _JTE_hookContext_exceptionThrown)
                     *     Hooks.invoke(Notify, _JTE_hookContext_exceptionThrown)
                     *   }
                     */

                    // 1. get the AST for the code in the comment above without the template
                    BlockStatement wrapper = getTemplateWrapperAST()
                    // 2. inject the user's pipeline template statements
                    wrapper.getStatements()[1].getTryStatement().addStatements(statements)
                    // 3. replace the compiled template with our new AST
                    statements.clear()
                    statements.add(0, wrapper)
                }

                BlockStatement getTemplateWrapperAST() {
                    List<ASTNode> statements = new AstBuilder().buildFromSpec {
                        block {
                            expression{
                                declaration{
                                    variable "_JTE_hookContext_exceptionThrown"
                                    token "="
                                    constant false
                                }
                            }
                            tryCatch {
                                block {
                                    expression {
                                        staticMethodCall(HooksWrapper, 'invoke') {
                                            argumentList {
                                                classExpression Validate
                                            }
                                        }
                                    }
                                    expression {
                                        staticMethodCall(HooksWrapper, 'invoke') {
                                            argumentList {
                                                classExpression Init
                                            }
                                        }
                                    }
                                }
                                block {
                                    block {
                                        expression {
                                            staticMethodCall(HooksWrapper, 'invoke') {
                                                argumentList {
                                                    classExpression CleanUp
                                                    variable "_JTE_hookContext_exceptionThrown"
                                                }
                                            }
                                        }
                                        expression {
                                            staticMethodCall(HooksWrapper, 'invoke') {
                                                argumentList {
                                                    classExpression Notify
                                                    variable "_JTE_hookContext_exceptionThrown"
                                                }
                                            }
                                        }
                                    }
                                }
                                catchStatement {
                                    parameter 'e': Exception
                                    block{
                                        expression{
                                            binary{
                                                variable "_JTE_hookContext_exceptionThrown"
                                                token "="
                                                constant true
                                            }
                                        }
                                        throwStatement {
                                            variable 'e'
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return statements.first()
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

                boolean isTemplate(ClassNode classNode) {
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
