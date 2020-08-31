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

import hudson.Extension
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates ApplicationEnvironments and populates the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class ApplicationEnvironmentInjector extends TemplatePrimitiveInjector {

    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("ApplicationEnvironment.groovy").text
        return parseClass(classText)
    }

    @SuppressWarnings('NoDef')
    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        Class appEnvClass = getPrimitiveClass()
        ArrayList createdEnvs = []
        config.getConfig().application_environments.each{ name, appEnvConfig ->
            def env = appEnvClass.newInstance(name, appEnvConfig)
            createdEnvs << env
            binding.setVariable(name, env)
        }
        createdEnvs.eachWithIndex{ env, index ->
            def previous = index ? createdEnvs[index - 1] : null
            def next = (index == (createdEnvs.size() - 1)) ? null : createdEnvs[index + 1]
            env.setPrevious(previous)
            env.setNext(next)
        }
    }

}
