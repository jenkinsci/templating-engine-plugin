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
package org.boozallen.plugins.jte.util

import hudson.Extension
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext
import org.boozallen.plugins.jte.init.primitives.hooks.HooksWrapper
import org.boozallen.plugins.jte.init.primitives.injectors.LibraryNamespace
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector
import org.boozallen.plugins.jte.init.primitives.injectors.StepContext
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperScript
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.AbstractWhitelist
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationBuilder
import org.boozallen.plugins.jte.init.governance.config.dsl.DslEnvVar

import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * Whitelists JTE primitives as permitted within the Jenkins pipeline sandbox.
 */
@Extension class SandboxWhitelist extends AbstractWhitelist {

    private final List<String> permittedReceiverStrings = [
        "org.boozallen.plugins.jte.init.primitives.hooks.Hooks",
        "org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperCPS"
    ]

    private final List<Class> permittedReceivers = [
            TemplatePrimitive,
            TemplatePrimitiveCollector.JTEVar,
            TemplatePrimitiveNamespace,
            LibraryNamespace,
            HooksWrapper,
            PipelineConfigurationBuilder,
            DslEnvVar,
            StepWrapperScript,
            HookContext,
            StepContext,
            StageInjector.StageContext
    ]

    @Override
    boolean permitsMethod(Method method, Object receiver, Object[] args) {
        boolean a = permittedReceivers.collect{ r -> receiver in r }.contains(true)
        boolean b = receiver.getClass().getName() in permittedReceiverStrings
        return (a || b)
    }

    @Override
    boolean permitsStaticMethod(Method method, Object[] args){
        Class receiver = method.getDeclaringClass()

        Class receivingClass
        if (args.size()){
            receivingClass = args[0].getClass()
        }

        boolean a = permittedReceivers.collect{ r -> receiver in r }.contains(true)
        boolean b = receiver.getName() in permittedReceiverStrings
        boolean c = args.size() && permittedReceivers.collect{ r -> receivingClass in r }.contains(true)
        boolean d = args.size() && receivingClass.getName() in permittedReceiverStrings
        return (a || b || c || d)
    }

    @Override
    boolean permitsConstructor(Constructor<?> constructor, Object[] args){
        return constructor.getDeclaringClass() == StepWrapperScript
    }

}
