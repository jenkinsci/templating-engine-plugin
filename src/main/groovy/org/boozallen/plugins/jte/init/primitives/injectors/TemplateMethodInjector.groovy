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
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution

/**
 * creates no-op steps based on the pipeline configuration
 */
@Extension class TemplateMethodInjector extends TemplatePrimitiveInjector {

    private static final String KEY = "template_methods"

    @SuppressWarnings("ParameterName")
    @Override
    @RunAfter([LibraryStepInjector, DefaultStepInjector])
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config){
        TemplatePrimitiveCollector primitiveCollector = getPrimitiveCollector(exec)
        TemplatePrimitiveNamespace steps = new TemplatePrimitiveNamespace(name: KEY)

        // populate namespace with no-op steps
        LinkedHashMap aggregatedConfig = config.getConfig()
        StepWrapperFactory stepFactory = new StepWrapperFactory(exec)
        aggregatedConfig[KEY].each{ step, _ ->
            if(!primitiveCollector.hasStep(step)){
                StepWrapper stepWrapper = stepFactory.createNullStep(step)
                stepWrapper.setParent(steps)
                steps.add(stepWrapper)
            }
        }

        return steps.getPrimitives() ? steps : null
    }

}
