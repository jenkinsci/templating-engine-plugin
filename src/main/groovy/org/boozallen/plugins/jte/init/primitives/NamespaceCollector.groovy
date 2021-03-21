package org.boozallen.plugins.jte.init.primitives

import groovy.text.Template
import hudson.Extension
import hudson.model.InvisibleAction
import hudson.model.Run
import hudson.util.DirScanner
import jenkins.security.CustomClassFilter
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun

class NamespaceCollector extends InvisibleAction{
    List<PrimitiveNamespace> namespaces = []

    void addNamespace(PrimitiveNamespace namespace){
        namespaces.add(namespace)
    }

    PrimitiveNamespace getNamespace(String name){
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
        Class clazz = StepWrapperFactory.getPrimitiveClass()
        return findAll{ primitive ->
            clazz.getName() == primitive.getClass().getName() &&
            primitive.getName() == name
        }
    }

    static PrimitiveNamespace createNamespace(String name){
        return new PrimitiveNamespace(name: name)
    }

    static class PrimitiveNamespace implements Serializable{
        String name
        List<GlobalVariable> primitives = []
        void add(GlobalVariable primitive){
            primitives.add(primitive)
        }
    }

    /**
     * During execution, this can be used to fetch the current
     * run's NamespaceCollector if present.
     *
     * @return the current run's NamespaceCollector. may be null
     */
    static NamespaceCollector current(){
        CpsThread thread = CpsThread.current()
        if(!thread){
            throw new IllegalStateException("CpsThread not present.")
        }
        FlowExecutionOwner flowOwner = thread.getExecution().getOwner()
        WorkflowRun run = flowOwner.run()
        return run.getAction(NamespaceCollector)
    }

    @Extension static class NamespaceProvider extends GlobalVariableSet{
        List<GlobalVariable> forRun(Run run){
            List<GlobalVariable> primitives = []
            if(run == null) return primitives
            NamespaceCollector namespaceCollector = run.getAction(NamespaceCollector)
            if(!namespaceCollector) return primitives
            namespaceCollector.getNamespaces().each{ namespace ->
                primitives.addAll(namespace.getPrimitives())
            }
            return primitives
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