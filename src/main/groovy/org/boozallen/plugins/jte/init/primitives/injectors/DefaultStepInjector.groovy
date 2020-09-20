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
import org.boozallen.plugins.jte.init.primitives.RunAfter
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Loads libraries from the pipeline configuration and injects StepWrapper's into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class DefaultStepInjector extends TemplatePrimitiveInjector {

    @Override
    @RunAfter(LibraryStepInjector)
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        LinkedHashMap aggregatedConfig = config.getConfig()
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        aggregatedConfig.steps.each{ stepName, stepConfig ->
            // if step already exists, print warning
            if (binding.hasStep(stepName)){
                ArrayList msg = [
                    "Configured step ${stepName} ignored.",
                    "-- Loaded by the ${binding.getStep(stepName).library} Library."
                ]
                logger.printWarning msg.join("\n")
            } else { // otherwise go ahead and create the default step implementation
                logger.print "Creating step ${stepName} from the default step implementation."
                binding.setVariable(stepName, stepFactory.createDefaultStep(binding, stepName, stepConfig))
            }
        }
    }

}
