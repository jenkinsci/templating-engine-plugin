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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.RunAfter
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates Stages
 */
@Extension class StageInjector extends TemplatePrimitiveInjector {

    private static final String KEY = "stages"

    @Override
    @RunAfter([LibraryStepInjector, DefaultStepInjector, TemplateMethodInjector])
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config){
        TemplatePrimitiveNamespace stages = new TemplatePrimitiveNamespace(name: KEY)

        // populate namespace with stages from pipeline config
        LinkedHashMap aggregatedConfig = config.getConfig()
        aggregatedConfig[KEY].each{ name, steps ->
            List<String> stepNames = steps.keySet() as List<String>
            Stage stage = new Stage(name, stepNames)
            stage.setParent(stages)
            stages.add(stage)
        }

        return stages.getPrimitives() ? stages : null
    }

    @Override
    void validatePrimitives(CpsFlowExecution exec, PipelineConfigurationObject config, TemplatePrimitiveCollector collector){
        FlowExecutionOwner flowOwner = exec.getOwner()
        LinkedHashMap aggregatedConfig = config.getConfig()
        LinkedHashMap stagesWithUndefinedSteps = [:]
        aggregatedConfig[KEY].each{ name, stageConfig ->
            List<String> steps = stageConfig.keySet() as List<String>
            List<String> undefinedSteps = []
            steps.each{ step ->
                if(!collector.hasStep(step)){
                    undefinedSteps << step
                }
            }
            if(undefinedSteps){
                stagesWithUndefinedSteps[name] = undefinedSteps
            }
        }
        if(stagesWithUndefinedSteps){
            List<String> error = [
                    "There are Stages defined that reference steps that do not exist.",
                    "Consider adding step names to the template_methods block"
            ]
            stagesWithUndefinedSteps.each{ name, steps ->
                error << "- ${name}: ${steps.join(", ")}".toString()
            }
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printError(error.join("\n"))
            throw new JTEException("There are Stages defined that require undefined steps")
        }
    }

    static class StageContext implements Serializable {
        private static final long serialVersionUID = 1L
        String name
        Map args = [:]
    }

}
