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

import hudson.FilePath
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.util.JTEException
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

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
    protected String library

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

    // flags to determine what type of step this is
    protected boolean isLibraryStep  = false
    protected boolean isDefaultStep  = false
    protected boolean isTemplateStep = false

    @Override String getName(){ return name }
    String getLibrary(){ return library }

    @SuppressWarnings("UnusedMethodParameter")
    Object getValue(CpsScript script, Boolean skipOverloaded = false){
        // if permissive_initialization is true, overloaded is okay
        if(! skipOverloaded){
            isOverloaded()
        }
        Class stepwrapperCPS = getPrimitiveClass()
        def s = stepwrapperCPS.newInstance(
            name: this.name,
            library: this.library,
            sourceText: this.sourceText,
            sourceFile: this.sourceFile,
            config: this.config,
            isLibraryStep: this.isLibraryStep,
            isDefaultStep: this.isDefaultStep,
            isTemplateStep: this.isTemplateStep,
            script: getScript()
        )
        s.getScript().setStageContext(this.stageContext)
        s.getScript().setHookContext(this.hookContext)
        return s
    }

    Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("StepWrapperCPS.groovy").text
        return TemplatePrimitiveInjector.parseClass(classText)
    }

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
        that.isLibraryStep = this.isLibraryStep
        that.isDefaultStep = this.isDefaultStep
        that.isTemplateStep = this.isTemplateStep
        that.script = getScript()
        that.script.setStageContext(this.stageContext)
        that.script.setHookContext(this.hookContext)
        return that
    }

    /*
     * memoized getter.
     * impl will be null:
     *  1. prior to first invocation
     *  2. after a pipeline is resumed following an ungraceful shut down
     */
    StepWrapperScript getScript(){
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

    /**
     * recompiles the StepWrapperScript if missing. This typically only happens if
     * Jenkins has ungracefully restarted and the pipeline is resuming
     * @return
     */
    StepWrapperScript parseSource(){
        CpsThread thread = CpsThread.current()
        if(!thread){
            throw new IllegalStateException("CpsThread not present.")
        }

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

        FlowExecutionOwner flowOwner = thread.getExecution().getOwner()
        StepWrapperFactory factory = new StepWrapperFactory(flowOwner)
        return factory.prepareScript(library, name, source, config, stageContext, hookContext)
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
