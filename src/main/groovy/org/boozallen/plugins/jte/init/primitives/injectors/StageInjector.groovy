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
import org.boozallen.plugins.jte.init.governance.config.dsl.TemplateConfigException
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates Stages and populates the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class StageInjector extends TemplatePrimitiveInjector {

    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("Stage.groovy").text
        return parseClass(classText)
    }

    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        Class stageClass = getPrimitiveClass()
        config.getConfig().stages.each{ name, steps ->
            ArrayList<String> stepsList = []
            steps.collect(stepsList){ step -> step.key }
            binding.setVariable(name, stageClass.newInstance(binding, name, stepsList))
        }
    }

    @Override
    void doPostInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        // 3. Inject a passthrough step for steps not defined (either as steps or other primitives)
        Map<String, List> missingSteps = [:]
        config.getConfig().stages.each{ name, steps ->
            steps.findAll{ step ->
                !(step.key in binding.registry)
            }.each{ step ->
                List missing = missingSteps[name] ?: []
                missing << step.key
                missingSteps[name] = missing
            }
        }

        if( missingSteps.size() > 0) {
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printError("----------------------------------")
            logger.printError("   Stage Configuration Errors   ")
            logger.printError("----------------------------------")
            missingSteps.each{ String stage, List names ->
                logger.printError("stage: ${stage}, the following steps were not found: " + names.join(","))
            }
            logger.printError("----------------------------------")
            throw new TemplateConfigException("There were missing steps for the stages.")
        }
    }

    static class StageContext implements Serializable {
        private static final long serialVersionUID = 1L
        String name
        Map args = [:]
    }

}
