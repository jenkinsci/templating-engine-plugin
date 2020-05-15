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
import jenkins.model.Jenkins
import jenkins.scm.api.SCMFile 
import org.boozallen.plugins.jte.init.primitives.TemplateException
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.util.RunUtils
import org.boozallen.plugins.jte.util.TemplateLogger
import org.boozallen.plugins.jte.util.TemplateScriptEngine
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.boozallen.plugins.jte.init.primitives.hooks.*

/*
    represents a library step. 

    this class serves as a wrapper class for the library step Script. 
    It's necessary for two reasons: 
    1. To give steps binding protection via TemplatePrimitive
    2. To provide a means to do LifeCycle Hooks before/after step execution
*/
class StepWrapper extends TemplatePrimitive implements Serializable{
    public static final String libraryConfigVariable = "config" 
    private Object impl
    private Binding binding
    private String name
    private String library 

    @NonCPS
    String getName(){ return name }
    
    @NonCPS
    String getLibrary(){ return library }
    
    /*
        need a call method defined on method missing so that 
        CpsScript recognizes the StepWrapper as something it 
        should execute in the binding. 
    */
    def call(Object... args){
        return invoke("call", args) 
    }

    /*
        all other method calls go through CpsScript.getProperty to 
        first retrieve the StepWrapper and then attempt to invoke a 
        method on it. 
    */
    def methodMissing(String methodName, args){
        return invoke(methodName, args)     
    }
    
    /*
        pass method invocations on the wrapper to the underlying
        step implementation script. 
    */
    def invoke(String methodName, Object... args){
        if(InvokerHelper.getMetaClass(impl).respondsTo(impl, methodName, args)){
            def result
            HookContext context = new HookContext(
                step: name, 
                library: library
            )
            try{
                Hooks.invoke(BeforeStep, binding, context)
                TemplateLogger.createDuringRun().print "[Step - ${library}/${name}.${methodName}(${args.collect{ it.getClass().simpleName }.join(", ")})]"
                result = InvokerHelper.getMetaClass(impl).invokeMethod(impl, methodName, args)
            } catch (Exception x) {
                throw new InvokerInvocationException(x)
            } finally{
                Hooks.invoke(AfterStep, binding, context)
                Hooks.invoke(Notify, binding, context)
            }
            return result 
        }else{
            throw new TemplateException("Step ${name} from the library ${library} does not have the method ${methodName}(${args.collect{ it.getClass().simpleName }.join(", ")})")
        }
    }

    void throwPreLockException(){
        throw new TemplateException ("Library Step Collision. The step ${name} already defined via the ${library} library.")
    }

    void throwPostLockException(){
        throw new TemplateException ("Library Step Collision. The variable ${name} is reserved as a library step via the ${library} library.")
    }

    @NonCPS
    static StepWrapper createFromFile(SCMFile file, String library, Binding binding, Map libConfig){
        String name = file.getName() - ".groovy" 
        String stepText = file.contentAsString()
        return createFromString(stepText, binding, name, library, libConfig)
    }

    @NonCPS
    static StepWrapper createDefaultStep(Binding binding, String name, Map stepConfig){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String defaultImpl = uberClassLoader.loadClass(self).getResource("defaultStepImplementation.groovy").text
        if (!stepConfig.name) stepConfig.name = name 
        return createFromString(defaultImpl, binding, name, "Default Step Implementation", stepConfig) 
    }

    @NonCPS
    static StepWrapper createNullStep(String stepName, Binding binding){
        String nullImpl = "def call(){ println \"Step ${stepName} is not implemented.\" }"
        return createFromString(nullImpl, binding, stepName, "Null Step", [:])
    }

    @NonCPS
    static StepWrapper createFromString(String stepText, Binding binding, String name, String library, Map libConfig){
        Script impl = TemplateScriptEngine.parse(stepText, binding)
        impl.metaClass."get${StepWrapper.libraryConfigVariable.capitalize()}" << { return libConfig }
        impl.metaClass.getStageContext = {->  [ name: null, args: [:] ]}
        return new StepWrapper(binding: binding, impl: impl, name: name, library: library) 
    }
}