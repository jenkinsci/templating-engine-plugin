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
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * injects the aggregated pipeline configuration as a variable called pipelineConfig into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class PipelineConfigVariableInjector extends TemplatePrimitiveInjector {

    static final String VARIABLE = "pipelineConfig"

    @SuppressWarnings('NoDef')
    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        Class keywordClass = KeywordInjector.getPrimitiveClass()
        def pipelineConfig = keywordClass.newInstance(
            keyword: VARIABLE,
            value: config.getConfig(),
            preLockException: "Variable ${VARIABLE} reserved for accessing the aggregated pipeline configuration",
            postLockException: "Variable ${VARIABLE} reserved for accessing the aggregated pipeline configuration"
        )
        binding.setVariable(VARIABLE, pipelineConfig)
    }

}
