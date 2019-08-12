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

package org.boozallen.plugins.jte.binding.injectors

import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.binding.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 
import jenkins.model.Jenkins
import com.cloudbees.groovy.cps.NonCPS

@Extension class ApplicationEnvironmentInjector extends TemplatePrimitiveInjector {

    @NonCPS
    static void doInject(TemplateConfigObject config, CpsScript script){
        Class ApplicationEnvironment = getPrimitiveClass()
        ArrayList createdEnvs = [] 
        config.getConfig().application_environments.each{ name, appEnvConfig ->
            def env = ApplicationEnvironment.newInstance(name, appEnvConfig)
            createdEnvs << env 
            script.getBinding().setVariable(name, env)
        }
        createdEnvs.eachWithIndex{ env, index -> 
            def previous = index ? createdEnvs[index - 1] : null  
            def next = (index != (createdEnvs.size() - 1)) ? createdEnvs[index + 1] : null 
            env.setPrevious(previous)
            env.setNext(next)
        }
    }

    static Class getPrimitiveClass(){
        String self = "org.boozallen.plugins.jte.binding.injectors.ApplicationEnvironmentInjector"
        String classText = TemplatePrimitiveInjector.Impl.classLoader
                                    .loadClass(self)
                                    .getResource("ApplicationEnvironment.groovy")
                                    .text
        return TemplateScriptEngine.parseClass(classText)
    }
}

