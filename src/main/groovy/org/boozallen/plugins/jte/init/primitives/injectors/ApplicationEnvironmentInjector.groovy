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
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.JTEException
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution

/**
 * creates ApplicationEnvironments
 */
@Extension class ApplicationEnvironmentInjector extends TemplatePrimitiveInjector {

    static private final String KEY = "application_environments"

    @Override
    void validateConfiguration(CpsFlowExecution exec, PipelineConfigurationObject config){
        LinkedHashMap aggregatedConfig = config.getConfig()
        AggregateException errors = new AggregateException()
        aggregatedConfig[KEY].each { name, appEnvConfig ->
            if(!(appEnvConfig in Map)){
                errors.add(new JTEException("Configuration for Application Environment ${name} must be a Block but is a ${appEnvConfig.getClass().getSimpleName()}"))
            }
        }
        if(errors.size()){
            throw errors
        }
    }

    @SuppressWarnings('NoDef')
    @Override
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config) {
        TemplatePrimitiveNamespace appEnvs = new TemplatePrimitiveNamespace(name: KEY)

        // populate the namespace with application environments from pipeline config
        LinkedHashMap aggregatedConfig = config.getConfig()
        List<ApplicationEnvironment> createdEnvs = []
        aggregatedConfig[KEY].each { name, appEnvConfig ->
            ApplicationEnvironment env = new ApplicationEnvironment(name, appEnvConfig)
            env.setParent(appEnvs)
            createdEnvs << env
        }
        createdEnvs.eachWithIndex { env, index ->
            ApplicationEnvironment previous = index ? createdEnvs[index - 1] : null
            ApplicationEnvironment next = (index == (createdEnvs.size() - 1)) ? null : createdEnvs[index + 1]
            env.setPrevious(previous)
            env.setNext(next)
            appEnvs.add(env)
        }

        return appEnvs.getPrimitives() ? appEnvs : null
    }

}
