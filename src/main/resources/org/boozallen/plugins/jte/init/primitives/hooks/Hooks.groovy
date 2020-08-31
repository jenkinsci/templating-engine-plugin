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
package org.boozallen.plugins.jte.init.primitives.hooks

import com.cloudbees.groovy.cps.NonCPS
import org.boozallen.plugins.jte.init.PipelineDecorator
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

import java.lang.annotation.Annotation

/**
 * Utility class to find and invoke lifecycle annotated methods within steps
 */
@SuppressWarnings("NoDef")
class Hooks implements Serializable{

    private static final long serialVersionUID = 1L

    @NonCPS
    static List<AnnotatedMethod> discover(Class<? extends Annotation> hookType, Binding binding){
        List<AnnotatedMethod> discovered = []
        Class stepWrapper = StepWrapperFactory.getPrimitiveClass()
        ArrayList stepWrappers = binding.getVariables().collect{ var -> var.value }.findAll{ var ->
            stepWrapper.getName() == var.getClass().getName()
        }

        stepWrappers.each{ step ->
            step.getScript().class.methods.each{ method ->
                def annotation = method.getAnnotation(hookType)
                if (annotation){
                    AnnotatedMethod am = new AnnotatedMethod(annotation, hookType.getSimpleName(), method.name, step)
                    discovered.push(am)
                }
            }
        }

        return discovered
    }

    static void invoke(Class<? extends Annotation> annotation, HookContext context = new HookContext()){
        CpsThread thread = CpsThread.current()
        if(!thread){
            throw new IllegalStateException("CpsThread not present.")
        }
        FlowExecutionOwner flowOwner = thread.getExecution().getOwner()
        WorkflowRun run = flowOwner.run()
        PipelineDecorator pipelineDecorator = run.getAction(PipelineDecorator)
        if(!pipelineDecorator){
            throw new IllegalStateException("PipelineDecorator action missing")
        }
        TemplateBinding binding = pipelineDecorator.getBinding()
        discover(annotation, binding).each{ hook ->
            if(shouldInvoke(hook, context)){
                hook.invoke(context)
            }
         }
    }

    @SuppressWarnings("MethodReturnTypeRequired")
    static shouldInvoke(AnnotatedMethod hook, HookContext context){
        def annotation = hook.getAnnotation()
        def stepWrapper = hook.getStepWrapper()
        Map config = stepWrapper.getScript().getConfig()
        RunWrapper currentBuild = getRunWrapper()
        Binding invokeBinding = new Binding([hookContext: context, config: config, currentBuild: currentBuild])
        def result
        try{
            result = annotation.value().newInstance(invokeBinding, invokeBinding).call()
        } catch(any){
            TemplateLogger.createDuringRun().printWarning "Exception thrown while evaluating @${hook.annotationName} on ${hook.methodName} in ${hook.stepWrapper.getName()} from ${hook.stepWrapper.getLibrary()} library."
            throw any
        }
        return result
    }

    static RunWrapper getRunWrapper(){
        CpsThread thread = CpsThread.current()
        if(!thread){
            throw new Exception("CpsThread not present.")
        }
        WorkflowRun run = thread.getExecution().getOwner().run()
        return new RunWrapper(run, true)
    }

}
