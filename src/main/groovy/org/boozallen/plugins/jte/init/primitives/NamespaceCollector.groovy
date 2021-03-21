package org.boozallen.plugins.jte.init.primitives

import hudson.Extension
import hudson.model.InvisibleAction
import hudson.model.Run
import jenkins.security.CustomClassFilter
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet

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
            return (c in TemplatePrimitiveGV) ?: null
        }

    }

}