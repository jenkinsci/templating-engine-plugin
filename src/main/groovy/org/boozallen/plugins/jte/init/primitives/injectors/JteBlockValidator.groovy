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
import org.boozallen.plugins.jte.init.JteBlockWrapper
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.ConfigValidator
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Validates the jte configuration block
 */
@Extension class JteBlockValidator extends TemplatePrimitiveInjector {

    static final String ERROR_MSG = "There were configuration errors in the jte block of the pipeline configuration"
    static final String ERROR_HEADER = "Pipeline Configuration JTE Block Errors:"

    @Override
    void validateConfiguration(CpsFlowExecution exec, PipelineConfigurationObject config){
        FlowExecutionOwner flowOwner = exec.getOwner()
        LinkedHashMap aggregatedConfig = config.getConfig()
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        if(aggregatedConfig.containsKey("jte")){
            ConfigValidator validator = new ConfigValidator(flowOwner)
            try{
                if(aggregatedConfig.jte instanceof Map){
                    validator.validate(JteBlockWrapper.getSchema(), aggregatedConfig.jte as LinkedHashMap)
                } else {
                    logger.printError(ERROR_HEADER)
                    logger.printError("1. jte field is expected to be a configuration block, found: ${aggregatedConfig.jte}")
                    throw new JTEException(ERROR_MSG)
                }
            } catch(AggregateException e){
                logger.printError(ERROR_HEADER)
                e.getExceptions().eachWithIndex{ error, i ->
                    logger.printError("${i + 1}: ${error.getMessage()}")
                }
                throw new JTEException(ERROR_MSG)
            }
        }
    }

}
