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

import com.cloudbees.groovy.cps.NonCPS
import hudson.FilePath
import org.boozallen.plugins.jte.init.PipelineDecorator
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplateException
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.hooks.Hooks
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.hooks.BeforeStep
import org.boozallen.plugins.jte.init.primitives.hooks.AfterStep
import org.boozallen.plugins.jte.init.primitives.hooks.Notify
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun

/**
 * A library step
 */
@SuppressWarnings("NoDef")
class StepWrapper extends TemplatePrimitive implements Serializable, Cloneable{

    private static final long serialVersionUID = 1L

    /**
     * The name of the step
     */
    String name

    /**
     * The name of the library that's contributed the step
     */
    String library

    /**
     * The injector that created this step
     */
    Class<? extends TemplatePrimitiveInjector> injector
    /**
     * The library configuration
     */
    private LinkedHashMap config

    /**
     * The FilePath where the source text for this
     * library step can be found
     */
    private String sourceFile

    /**
     * Alternatively, store the source text in a variable.
     *   e.g. NullStep's and Default Step Implementation
     */
    private String sourceText

    /**
     * A caching of the parsed source text used during invocation
     */
    private transient StepWrapperScript script

    /**
     * optional StageContext. assumes nondefault value of this step is
     * running as part of a Stage
     */
    private StageContext stageContext

    /**
     * optional HookContext. assumes nondefault value if this step was
     * invoked because of a lifecycle hook
     */
    private HookContext hookContext

    @NonCPS @Override String getName(){ return name }
    @NonCPS String getLibrary(){ return library }
    @NonCPS @Override Class<? extends TemplatePrimitiveInjector> getInjector(){ return injector }

    /**
     * clones this StepWrapper
     *
     * @return An equivalent StepWrapper instance
     */
    @SuppressWarnings("UnnecessaryObjectReferences")
    Object clone(){
        Object that = super.clone()
        that.name = this.name
        that.library = this.library
        that.sourceText = this.sourceText
        that.sourceFile = this.sourceFile
        that.config = this.config
        that.injector = this.injector
        return that
    }

    /*
     * memoized getter.
     * impl will be null:
     *  1. prior to first invocation
     *  2. after a pipeline is resumed following an ungraceful shut down
     */
    @NonCPS StepWrapperScript getScript(){
        script = script ?: parseSource()
        return script
    }

    void setStageContext(StageContext stageContext){
        this.stageContext = stageContext
        getScript().setStageContext(stageContext)
    }

    void setHookContext(HookContext hookContext){
        this.hookContext = hookContext
        getScript().setHookContext(hookContext)
    }

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
            HookContext context = new HookContext(step: name, library: library)
            try{
                Hooks.invoke(BeforeStep, context)
                TemplateLogger.createDuringRun().print "[Step - ${library}/${name}.${methodName}(${argsList})]"
                result = InvokerHelper.getMetaClass(getScript()).invokeMethod(getScript(), methodName, args)
            } catch (x) {
                throw new InvokerInvocationException(x)
            } finally{
                Hooks.invoke(AfterStep, context)
                Hooks.invoke(Notify, context)
            }
            return result
        }
        throw new TemplateException("Step ${name} from the library ${library} does not have the method ${methodName}(${argsList})")
    }

    @NonCPS
    void throwPreLockException(){
        throw new TemplateException ("Library Step Collision. The step ${name} already defined via the ${library} library.")
    }

    void throwPostLockException(){
        throw new TemplateException ("Library Step Collision. The variable ${name} is reserved as a library step via the ${library} library.")
    }

    /**
     * recompiles the StepWrapperScript if missing. This typically only happens if
     * Jenkins has ungracefully restarted and the pipeline is resuming
     * @return
     */
    @NonCPS
    private StepWrapperScript parseSource(){
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

        String source
        if(sourceFile){
            FilePath f = new FilePath(new File(sourceFile))
            if(f.exists()){
                source = f.readToString()
            } else{
                throw new IllegalStateException("Unable to find source file '${sourceFile}' for StepWrapper[library: ${library}, name: ${name}]")
            }
        } else if (sourceText){
            source = sourceText
        } else{
            throw new IllegalStateException("Unable to determine StepWrapper[library: ${library}, name: ${name}] source.")
        }

        StepWrapperFactory factory = new StepWrapperFactory(flowOwner)
        return factory.prepareScript(library, name, source, binding, config, stageContext, hookContext)
    }

}
