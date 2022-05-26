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

import org.boozallen.plugins.jte.init.primitives.TemplateException
import org.boozallen.plugins.jte.init.primitives.hooks.Hooks
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.hooks.BeforeStep
import org.boozallen.plugins.jte.init.primitives.hooks.AfterStep
import org.boozallen.plugins.jte.init.primitives.hooks.Notify
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException

/**
 * A library step
 */
@SuppressWarnings("NoDef")
class StepWrapperCPS implements Serializable{

    private static final long serialVersionUID = 1L

    StepWrapper parent

    @SuppressWarnings(["MethodReturnTypeRequired", "MethodParameterTypeRequired"])
    def methodMissing(String methodName, args){
        if(parent == null){
            throw new IllegalStateException("StepWrapperCPS does not have a StepWrapper parent")
        }

        String library = parent.library
        String name = parent.name
        // pass parent StepWrapper contexts to the script so they can be resolved during step execution
        StepWrapperScript script = parent.script
        script.setStepContext(parent.stepContext)
        script.setStageContext(parent.stageContext)

        String argsList = args.collect{ arg -> arg.getClass().simpleName }.join(", ")
        if(InvokerHelper.getMetaClass(script).respondsTo(script, methodName, args)){
            def result
            HookContext context = new HookContext(step: name, library: library, methodName: methodName)
            script.setHookContext(context)
            try{
                Hooks.invoke(BeforeStep, context)
                TemplateLogger.createDuringRun().print "[Step - ${library}/${name}.${methodName}(${argsList})]"
                result = InvokerHelper.getMetaClass(script).invokeMethod(script, methodName, args)
            } catch (x) {
                context.exceptionThrown = true
                throw new InvokerInvocationException(x)
            } finally{
                Hooks.invoke(AfterStep, context)
                Hooks.invoke(Notify, context)
            }
            return result
        }
        throw new TemplateException("Step ${name} from the library ${library} does not have the method ${methodName}(${argsList})")
    }

}
