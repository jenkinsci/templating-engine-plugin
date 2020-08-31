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
import org.boozallen.plugins.jte.init.governance.config.dsl.TemplateConfigException
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Loads libraries from the pipeline configuration and injects StepWrapper's into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class LibraryLoader extends TemplatePrimitiveInjector {

    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        // 1. Inject steps from loaded libraries
        WorkflowJob job = flowOwner.run().getParent()
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)
        List<LibrarySource> libs = tiers.collect{ tier ->
            tier.getLibrarySources()
        }.flatten() - null
        List<LibraryProvider> providers = libs.collect{ libSource ->
            libSource.getLibraryProvider()
        } - null

        ArrayList libConfigErrors = []
        config.getConfig().libraries.each{ libName, libConfig ->
            LibraryProvider p = providers.find{ provider ->
                provider.hasLibrary(flowOwner, libName)
            }
            if (p){
                libConfigErrors << p.loadLibrary(flowOwner, binding, libName, libConfig)
            } else {
                libConfigErrors << "Library ${libName} Not Found."
            }
        }
        libConfigErrors = libConfigErrors.flatten() - null

        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        // if library errors were found:
        if(libConfigErrors){
            logger.printError("----------------------------------")
            logger.printError("   Library Configuration Errors   ")
            logger.printError("----------------------------------")
            libConfigErrors.each{ line ->
                logger.printError(line)
            }
            logger.printError("----------------------------------")
            throw new TemplateConfigException("There were library configuration errors.")
        }

        // 2. Inject steps with default step implementation for configured step
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        config.getConfig().steps.findAll{ stepName, stepConfig ->
            if (binding.hasStep(stepName)){
                ArrayList msg = [
                    "Configured step ${stepName} ignored.",
                    "-- Loaded by the ${binding.getStep(stepName).library} Library."
                ]
                logger.printWarning msg.join("\n")
                return false
            }
            return true
        }.each{ stepName, stepConfig ->
            logger.print "Creating step ${stepName} from the default step implementation."
            binding.setVariable(stepName, stepFactory.createDefaultStep(binding, stepName, stepConfig))
        }
    }

    @Override
    void doPostInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        // 3. Inject a passthrough step for steps not defined (either as steps or other primitives)
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        config.getConfig().template_methods.findAll{ step ->
            !(step.key in binding.registry)
        }.each{ step ->
            binding.setVariable(step.key, stepFactory.createNullStep(step.key, binding))
        }
    }

}
