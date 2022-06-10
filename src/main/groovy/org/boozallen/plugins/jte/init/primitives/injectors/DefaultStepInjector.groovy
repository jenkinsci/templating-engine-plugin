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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates instances of the default step implementation
 */
@Extension class DefaultStepInjector extends TemplatePrimitiveInjector {

    private static final String KEY = "steps"

    @Override
    @RunAfter(LibraryStepInjector)
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config){
        FlowExecutionOwner flowOwner = exec.getOwner()
        TemplatePrimitiveCollector primitiveCollector = getPrimitiveCollector(exec)
        TemplatePrimitiveNamespace steps = new TemplatePrimitiveNamespace(name: KEY)

        // populate namespace with default steps from pipeline config
        LinkedHashMap aggregatedConfig = config.getConfig()
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        StepWrapperFactory stepFactory = new StepWrapperFactory(exec)
        aggregatedConfig[KEY].each{ stepName, stepConfig ->
            // if step already exists, print warning
            if (primitiveCollector.hasStep(stepName)){
                ArrayList msg = [
                    "Configured step ${stepName} ignored.",
                    "-- Loaded by the ${primitiveCollector.getSteps(stepName).library} Library."
                ]
                logger.printWarning msg.join("\n")
            } else { // otherwise go ahead and create the default step implementation
                logger.print "Creating step ${stepName} from the default step implementation."
                StepWrapper step = stepFactory.createDefaultStep(stepName, stepConfig)
                step.setParent(steps)
                steps.add(step)
            }
        }

        return steps.getPrimitives() ? steps : null
    }

}
