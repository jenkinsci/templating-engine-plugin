package org.boozallen.plugins.jte.init.primitives

import hudson.AbortException

import javax.annotation.Nonnull

class JteNamespace extends TemplatePrimitive implements Serializable{

    private static final long serialVersionUID = 1L

    List<Namespace> namespaces = []

    @Override String getName(){ return null }
    @Override Class getInjector(){ return null }

    @Override
    void throwPreLockException() {
        throw new Exception("prelock")
    }

    @Override
    void throwPostLockException() {
        throw new Exception("postlock")
    }

    Object getProperty(String property){
        Namespace namespace = namespaces.find{ n ->
            n.getName() == property
        }
        if(!namespace){
            throw new AbortException("JTE does not have a primitive namespace for ${property}")
        }
        return namespace
    }

    Namespace getNamespace(String name){
        return namespaces.find{ n -> n.getName() == name }
    }

    void addNamespace(Namespace namespace){
        if(getNamespace(namespace.getName())){
            throw new Exception("JTE already has primitive namespace ${namespace.getName()}")
        }
        namespaces.push(namespace)
    }

    static abstract class Namespace implements Serializable{
        private static final long serialVersionUID = 1L
        String name
        abstract void push(TemplatePrimitive primitive)
    }

}
