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

import groovy.transform.AutoClone
import hudson.FilePath
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.util.JTEException
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.CpsThread

/**
 * A library step
 */
@SuppressWarnings("NoDef")
@AutoClone
class StepWrapper extends TemplatePrimitive implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * The library configuration
     */
    protected LinkedHashMap config

    /**
     * The FilePath where the source text for this
     * library step can be found
     */
    protected String sourceFile

    /**
     * Alternatively, store the source text in a variable.
     *   e.g. NullStep's and Default Step Implementation
     */
    protected String sourceText

    /**
     * A caching of the parsed source text used during invocation
     */
    transient StepWrapperScript script

    /**
     * optional StageContext. assumes nondefault value of this step is
     * running as part of a Stage
     */
    protected StageContext stageContext

    /**
     * optional HookContext. assumes nondefault value if this step was
     * invoked because of a lifecycle hook
     */
    protected HookContext hookContext

    /**
     * current step's metadata
     */
    protected StepContext stepContext

    // flags to determine what type of step this is
    protected boolean isLibraryStep  = false
    protected boolean isDefaultStep  = false
    protected boolean isTemplateStep = false
    protected boolean isSandboxed = true

    @SuppressWarnings("UnusedMethodParameter")
    Object getValue(CpsScript script, Boolean skipOverloaded = false){
        // if permissive_initialization is true, overloaded is okay
        if(! skipOverloaded){
            isOverloaded()
        }
        Class stepwrapperCPS = getPrimitiveClass()
        def s = stepwrapperCPS.newInstance(parent: this)
        return s
    }

    Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("StepWrapperCPS.groovy").text
        return TemplatePrimitiveInjector.parseClass(classText)
    }

    /*
     * memoized getter.
     * impl will be null after a pipeline is resumed following an ungraceful shut down
     */
    StepWrapperScript getScript(CpsFlowExecution exec = null){
        script = script ?: parseSource(exec)
        return script
    }

    void setStageContext(StageContext stageContext){
        this.stageContext = stageContext
    }

    void setHookContext(HookContext hookContext){
        this.hookContext = hookContext
    }

    void setStepContext(StepContext stepContext){
        this.stepContext = stepContext
    }

    String getName(){
        return stepContext.name
    }

    String getLibrary(){
        return stepContext.library
    }

    void setSandboxed(boolean sandboxed){
        this.isSandboxed = sandboxed
    }

    /**
     * recompiles the StepWrapperScript if missing. This typically only happens if
     * Jenkins has ungracefully restarted and the pipeline is resuming
     * @return
     */
    StepWrapperScript parseSource(CpsFlowExecution exec = null){
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

        CpsFlowExecution e = exec
        if(e == null){
            CpsThread thread = CpsThread.current()
            if(!thread){
                throw new IllegalStateException("CpsThread not present.")
            }
            e = thread.getExecution()
        }
        StepWrapperFactory factory = new StepWrapperFactory(e)
        return factory.prepareScript(this, source)
    }

    @Override String toString(){
        if(isLibraryStep){
            return "Step '${name}' from the '${library}' Library"
        } else if (isDefaultStep){
            return "Default Step Implementation '${name}'"
        } else if (isTemplateStep){
            return "No-Op Template Step '${name}'"
        }
        throw new JTEException("StepWrapper origin could not be found")
    }

}
