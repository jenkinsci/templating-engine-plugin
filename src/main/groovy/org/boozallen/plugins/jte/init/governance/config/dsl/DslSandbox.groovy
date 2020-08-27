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
package org.boozallen.plugins.jte.init.governance.config.dsl

import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.kohsuke.groovy.sandbox.GroovyInterceptor
import org.kohsuke.groovy.sandbox.GroovyInterceptor.Invoker

/**
 * Groovy Interceptor that throws an exception if a user tries to do anything beyond
 * what is allowed from the pipeline configuration DSL.
 */
class DslSandbox extends GroovyInterceptor {

    Script script
    EnvActionImpl env

    DslSandbox(Script script, EnvActionImpl env){
        this.script = script
        this.env = env
    }

    @Override
    Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        if (!(receiver == script)) {
            throw new SecurityException("""
            onMethodCall:
            invoker -> ${invoker}
            receiver -> ${receiver}
            method -> ${method}
            args -> ${args}
            """.trim().stripIndent())
        }
        return invoker.call(receiver, method, args)
    }

    @Override
    Object onStaticCall(Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
        throw new SecurityException("""
        onStaticCall:
        invoker -> ${invoker}
        receiver -> ${receiver}
        method -> ${method}
        args -> ${args}
        """.trim().stripIndent())
    }

    @Override
    Object onNewInstance(Invoker invoker, Class receiver, Object... args) throws Throwable {
        if(!(receiver == script)){
            throw new SecurityException("""
            onNewInstance:
            invoker -> ${invoker}
            receiver -> ${receiver}
            args -> ${args}
            """.trim().stripIndent())
        }
    }

    @Override
    void onSuperConstructor(Invoker invoker, Class receiver, Object... args) throws Throwable {
        if (!(receiver == script)){
            throw new SecurityException("""
            onSuperConstructor:
            invoker -> ${invoker}
            receiver -> ${receiver}
            args -> ${args}
            """.trim().stripIndent())
        }
    }

    @Override
    Object onSuperCall(Invoker invoker, Class senderType, Object receiver, String method, Object... args) throws Throwable {
        throw new SecurityException("""
        onSuperCall:
        invoker -> ${invoker}
        senderType -> ${senderType}
        receiver -> ${receiver}
        method -> ${method}
        args -> ${args}
        """.trim().stripIndent())
    }

    @Override
    Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        if (!(receiver == script || receiver == env)){
            throw new SecurityException("""
            onGetProperty:
            invoker -> ${invoker}
            receiver -> ${receiver}
            property -> ${property}
            """.trim().stripIndent())
        }
        return invoker.call(receiver, property)
    }

    @Override
    Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        if (!(receiver == script)){
            throw new SecurityException("""
            onSetProperty:
            invoker -> ${invoker}
            receiver -> ${receiver}
            property -> ${property}
            value -> ${value}
            """.trim().stripIndent())
        }
        return invoker.call(receiver, property, value)
    }

    @Override
    Object onGetAttribute(Invoker invoker, Object receiver, String attribute) throws Throwable {
        throw new SecurityException("""
        onGetAttribute:
        invoker -> ${invoker}
        receiver -> ${receiver}
        attribute -> ${attribute}
        """.trim().stripIndent())
    }

    @Override
    Object onSetAttribute(Invoker invoker, Object receiver, String attribute, Object value) throws Throwable {
        throw new SecurityException("""
        onSetAttribute:
        invoker -> ${invoker}
        receiver -> ${receiver}
        attribute -> ${attribute}
        value -> ${value}
        """.trim().stripIndent())
    }

    @Override
    Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
        throw new SecurityException("""
        onGetArray:
        invoker -> ${invoker}
        receiver -> ${receiver}
        index -> ${index}
        """.trim().stripIndent())
    }

    @Override
    Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
        throw new SecurityException("""
        onSetArray:
        invoker -> ${invoker}
        receiver -> ${receiver}
        index -> ${index}
        value -> ${value}
        """.trim().stripIndent())
    }

}
