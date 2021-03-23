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
import hudson.model.Run
import jenkins.model.RunAction2
import jenkins.security.CustomClassFilter
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapper
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.JTEException
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun

import javax.annotation.Nonnull

class TemplatePrimitiveCollector extends InvisibleAction{

    List<TemplatePrimitiveNamespace> namespaces = []

    void addNamespace(TemplatePrimitiveNamespace namespace){
        namespaces.add(namespace)
    }

    TemplatePrimitiveNamespace getNamespace(String name){
        return namespaces.find{namespace ->
            namespace.getName() == name
        }
    }

    static List<GlobalVariable> getGlobalVariablesByName(String name, Run run){
        return GlobalVariable.forRun(run).findAll{ variable ->
            variable.getName() == name
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
        getNamespaces().each{namespace ->
            primitives.addAll( namespace.getPrimitives().findAll(condition) )
        }
        return primitives
    }

    boolean hasStep(String name){
        return getStep(name) as boolean
    }

    List<TemplatePrimitive> getStep(String name){
        return findAll{ primitive ->
            primitive instanceof StepWrapper &&
            primitive.getName() == name
        }
    }

    List<TemplatePrimitive> getPrimitives(){
        return findAll{ true }
    }

    // FIXME: this doesn't belong here anymore. just use the constructor.
    static TemplatePrimitiveNamespace createNamespace(String name){
        return new TemplatePrimitiveNamespace(name: name)
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

    /**
     * exposes the primitives populated on this action to the Run
     */
    @Extension static class TemplatePrimitiveProvider extends GlobalVariableSet{
        List<GlobalVariable> forRun(Run run){
            List<GlobalVariable> primitives = []
            if(run == null) return primitives
            TemplatePrimitiveCollector primitiveCollector = run.getAction(TemplatePrimitiveCollector)
            /* the run might not belong to JTE */
            if(!primitiveCollector) return primitives
            primitives.addAll(primitiveCollector.getPrimitives())
            primitives.add(new TemplatePrimitiveCollector.JTEVar())
            return primitives
        }
    }

    static class JTEVar extends GlobalVariable{
        @Override
        String getName() {
            return "jte"
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

}
