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
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

import javax.annotation.CheckForNull

/**
 * Decorates the pipeline template during compilation
 * <p>
 * modifies the template at compile time to invoke lifecycle hooks prior to and after template execution
 *
 * @author Steven Terrana
 */
@Extension(ordinal=1.0D) // set ordinal > 0 so JTE comes before Declarative
class PipelineTemplateCompiler extends GroovyShellDecorator {

    /**
     * For the JTE pipeline template, customizes the compiler so that the template is wrapped
     * in a try-finally block so that the @Validation and @Init Lifecycle Hooks can be invoked
     * prior to template execution and @CleanUp and @Notify Lifecycle Hooks can be invoked post
     * template execution.
     */
    @Override
    @SuppressWarnings("MethodSize")
    void configureCompiler(@CheckForNull final CpsFlowExecution execution, CompilerConfiguration cc) {
        if(execution == null){
            return
        }
        // ensure this is a JTE pipeline
        FlowExecutionOwner flowOwner = execution.getOwner()
        WorkflowRun run = flowOwner.run()
        WorkflowJob job = run.getParent()
        if(!(job.getDefinition() in TemplateFlowDefinition)){
            return
        }
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
