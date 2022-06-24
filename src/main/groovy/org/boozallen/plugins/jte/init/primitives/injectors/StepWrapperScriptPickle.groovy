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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.pickles.Pickle
import org.jenkinsci.plugins.workflow.support.pickles.SingleTypedPickleFactory
import org.jenkinsci.plugins.workflow.support.pickles.TryRepeatedly
import com.google.common.util.concurrent.ListenableFuture

/**
 * responsible for rehydrating compiled steps
 * required to successfully resume a pipeline that
 * restarts mid-step execution
 */
class StepWrapperScriptPickle extends Pickle{

    private final StepContext stepContext
    private final HookContext hookContext
    private final StageContext stageContext

    private StepWrapperScriptPickle(StepWrapperScript script){
        stepContext = script.getStepContext()
        hookContext = script.getHookContext()
        stageContext = script.getStageContext()
    }

    @Override
    ListenableFuture<StepWrapperScript> rehydrate(FlowExecutionOwner flowOwner){
        return new TryRepeatedly<StepWrapperScript>(1, 0){
            @Override
            protected StepWrapperScript tryResolve(){
                if(flowOwner == null){
                    return null
                }
                WorkflowRun run = flowOwner.run()
                TemplatePrimitiveCollector jte = run.getAction(TemplatePrimitiveCollector)
                if (jte == null){
                    throw new IllegalStateException("Unable to unmarshal StepWrapperScript")
                }
                List<TemplatePrimitive> steps = jte.getSteps(stepContext.name)
                StepWrapper step = steps.find{ StepWrapper step ->
                    step.getName() == stepContext.name &&
                    step.getLibrary() == stepContext.library
                }
                if (step == null){
                    throw new IllegalStateException("Unable to determine StepWrapper")
                }
                StepWrapperScript script = step.getScript(flowOwner.get())
                script.setStepContext(stepContext)
                script.setHookContext(hookContext)
                script.setStageContext(stageContext)
                return script
            }
        }
    }

    @Extension
    static final class Pickler extends SingleTypedPickleFactory<StepWrapperScript>{
        @Override
        protected Pickle pickle(StepWrapperScript script){
            return new StepWrapperScriptPickle(script)
        }
    }

}
