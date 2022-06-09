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
package org.boozallen.plugins.jte.init.primitives

import hudson.Extension
import hudson.model.InvisibleAction
import hudson.model.Job
import hudson.model.Run
import hudson.model.TaskListener
import jenkins.security.CustomClassFilter
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapper
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.DSL
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet
import org.jenkinsci.plugins.workflow.flow.FlowCopier
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun

import javax.annotation.Nonnull

/**
 * Stores {@see TemplatePrimitive}s that have been created for a run
 */
class TemplatePrimitiveCollector extends InvisibleAction{

    /**
     * The common classloader that's used across Pipeline Template
     * and individual StepWrapper compilation. It gets added by
     * {@see StepWrapperFactory}. While not serializable, as an
     * action, this class is persisted to the run's build.xml file
     */
    @SuppressWarnings('UnnecessaryTransientModifier')
    transient ClassLoader loader
    List<TemplatePrimitiveNamespace> namespaces = []

    static List<GlobalVariable> getGlobalVariablesByName(String name, Run run){
        return GlobalVariable.forRun(run).findAll{ variable ->
            variable.getName() == name
        }
    }

    /**
     * During execution, this can be used to fetch the current
     * run's TemplatePrimitiveCollector if present.
     *
     * @return the current run's TemplatePrimitiveCollector. may be null
     */
    static TemplatePrimitiveCollector current(){
        CpsThread thread = CpsThread.current()
        if(!thread){
            throw new IllegalStateException("CpsThread not present.")
        }
        FlowExecutionOwner flowOwner = thread.getExecution().getOwner()
        WorkflowRun run = flowOwner.run()
        return run.getAction(TemplatePrimitiveCollector)
    }

    @SuppressWarnings("ReturnNullFromCatchBlock") // that's literally the point of this method
    static TemplatePrimitiveCollector currentNoException(){
        try{
            return current()
        } catch(IllegalStateException e){
            return null
        }
    }

    void addNamespace(TemplatePrimitiveNamespace namespace){
        namespaces.add(namespace)
    }

    TemplatePrimitiveNamespace getNamespace(String name){
        return namespaces.find{ namespace ->
            namespace.getName() == name
        }
    }

    Set<String> getPrimitiveNames(){
        Set<String> primitives = []
        getNamespaces().each{ namespace ->
            primitives.addAll(namespace.getPrimitives()*.getName())
        }
        return primitives
    }

    List<TemplatePrimitive> findAll(Closure condition){
        List<TemplatePrimitive> primitives = []
        getNamespaces().each{ namespace ->
            primitives.addAll( namespace.getPrimitives().findAll(condition) )
        }
        return primitives
    }

    boolean hasStep(String name){
        return getSteps(name) as boolean
    }

    List<TemplatePrimitive> getSteps(String name){
        return findAll{ primitive ->
            primitive instanceof StepWrapper &&
            primitive.getName() == name
        }
    }

    List<TemplatePrimitive> getPrimitives(){
        return findAll{ true }
    }

    /**
     * exposes the primitives populated on this action to the Run
     */
    @Extension static class TemplatePrimitiveProvider extends GlobalVariableSet{
        List<GlobalVariable> forRun(Run run){
            List<GlobalVariable> primitives = []
            if(run == null){
                return primitives
            }
            TemplatePrimitiveCollector primitiveCollector = run.getAction(TemplatePrimitiveCollector)
            /* the run might not belong to JTE */
            if(!primitiveCollector){
                return primitives
            }
            primitives.addAll(primitiveCollector.getPrimitives())
            primitives.add(new TemplatePrimitiveCollector.JTEVar())
            primitives.add(new TemplatePrimitiveCollector.StepsVar())
            return primitives
        }

        /**
         * The forRun method is used to fetch variables everywhere that it matters
         *
         * Right now, the only way to exclude JTE variables from the /pipeline-syntax/globals page
         * is to return an empty list from forJob
         *
         * @param job the job to fetch global variables for
         * @return an empty list of global variables
         */
        @Override
        List<GlobalVariable> forJob(Job job){
            return []
        }
    }

    static class StepsVar extends GlobalVariable{
        static final String KEY = "steps"

        DSL dsl

        @Override
        String getName(){
            return KEY
        }

        @Override
        Object getValue(CpsScript script){
            if(dsl){ return dsl }
            WorkflowRun run = script.$buildNoException()
            dsl = new DSL(run.asFlowExecutionOwner())
            return dsl
        }

    }

    static class JTEVar extends GlobalVariable{
        static final String KEY = "jte"
        @Override
        String getName() {
            return KEY
        }

        @Override
        Object getValue(@Nonnull CpsScript script) throws Exception {
            return this
        }

        Object getProperty(String property){
            TemplatePrimitiveCollector collector = TemplatePrimitiveCollector.current()
            TemplatePrimitiveNamespace namespace = collector.getNamespace(property)
            if(namespace){
                return namespace
            }
            throw new JTEException("JTE does not have Template Namespace ${property}")
        }
    }

    /**
     * Allows TemplatePrimitives to be stored on this action without
     * triggering an Unmarshalling exception.
     *
     * see https://github.com/jenkinsci/jep/blob/master/jep/200/README.adoc#extensibility
     * for more information
     */
    @Extension
    static class CustomClassFilterImpl implements CustomClassFilter {
        @SuppressWarnings('BooleanMethodReturnsNull')
        @Override Boolean permits(Class<?> c){
            return (c in TemplatePrimitive) ?: null
        }
    }

    /**
     * For replayed or restarted jobs, need to copy over the original Run's
     * TemplatePrimitiveCollector to skip initialization but preserve primitives
     */
    @Extension
    static class CopyTemplatePrimitiveCollector extends FlowCopier.ByRun {
        @Override void copy(Run<?,?> original, Run<?,?> copy, TaskListener listener){
            TemplatePrimitiveCollector collector = original.getAction(TemplatePrimitiveCollector)
            if(collector != null){
                TemplateLogger logger = new TemplateLogger(listener)
                logger.print("Copying loaded primitives from previous run")

                /*
                 * TBH i don't understand why this is necessary myself.
                 *
                 * When restarting a declarative pipeline, without this code, the following exception is thrown:
                 *   hudson.remoting.ProxyException: java.io.IOException: cannot find current thread
                 *
                 * if i had to guess, it's because StepWrapperFactory creates a fake CpsFlowExecution to parse the
                 * step based on the now PREVIOUS run.  So when re-running the step, that run is completed and the
                 * thread is gone.
                 *
                 * By setting the StepWrapper's script to null, we force a recompilation of the user-provided script
                 * which sets the thread to this new run's execution, _i think_.
                 */
                collector.findAll{ primitive ->
                    primitive instanceof StepWrapper
                }.each{ step ->
                    step.script = null
                }

                copy.replaceAction(collector)
            }
        }
    }

}
