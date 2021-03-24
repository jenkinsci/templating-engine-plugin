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
class StepWrapperCPS extends StepWrapper{

    /*
        need a call method defined on method missing so that
        CpsScript recognizes the StepWrapper as something it
        should execute in the binding.
    */
    @SuppressWarnings("MethodReturnTypeRequired")
    def call(Object... args) {
        return invoke("call", args)
    }

    /*
        all other method calls go through CpsScript.getProperty to
        first retrieve the StepWrapper and then attempt to invoke a
        method on it.
    */
    @SuppressWarnings(["MethodReturnTypeRequired", "MethodParameterTypeRequired"])
    def methodMissing(String methodName, args){
        return invoke(methodName, args)
    }

    /*
        pass method invocations on the wrapper to the underlying
        step implementation script.
    */
    @SuppressWarnings("MethodReturnTypeRequired")
    def invoke(String methodName, Object... args){
        String argsList = args.collect{ arg -> arg.getClass().simpleName }.join(", ")
        if(InvokerHelper.getMetaClass(getScript()).respondsTo(getScript(), methodName, args)){
            def result
            HookContext context = new HookContext(step: getName(), library: getLibrary())
            try{
                Hooks.invoke(BeforeStep, context)
                TemplateLogger.createDuringRun().print "[Step - ${getLibrary()}/${getName()}.${methodName}(${argsList})]"
                result = InvokerHelper.getMetaClass(getScript()).invokeMethod(getScript(), methodName, args)
            } catch (x) {
                throw new InvokerInvocationException(x)
            } finally{
                Hooks.invoke(AfterStep, context)
                Hooks.invoke(Notify, context)
            }
            return result
        }
        throw new TemplateException("Step ${getName()} from the library ${getLibrary()} does not have the method ${methodName}(${argsList})")
    }

}
