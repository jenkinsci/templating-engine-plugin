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
package org.boozallen.plugins.jte.init.primitives.hooks

import hudson.Extension
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateScriptEngine
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/*
 Hooks invoke other pipeline code so the class that gets called needs to be CPS
 transformed.  This injector just gets used as a hook to insert the Hooks and
 AnnotatedMethod class into the ClassLoader after being CPS Transformed via
 TemplateScriptEngine.parseClass
 */
@Extension class HookInjector extends TemplatePrimitiveInjector {

    static Class getHooksClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("Hooks.groovy").text
        return TemplateScriptEngine.parseClass(classText)
    }

    static Class getAnnotatedMethodClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("AnnotatedMethod.groovy").text
        return TemplateScriptEngine.parseClass(classText)
    }

    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        Class hooksClass = getHooksClass()
        getAnnotatedMethodClass()

        /*
         This seems random without context.
         Check out GroovyShellDecoratorImpl.configureCompiler to understand why
         this is being done.
         */
        binding.setVariable("Hooks", hooksClass)
        binding.setVariable("Validate", Validate)
        binding.setVariable("Init", Init)
        binding.setVariable("CleanUp", CleanUp)
        binding.setVariable("Notify", Notify)
    }

}
