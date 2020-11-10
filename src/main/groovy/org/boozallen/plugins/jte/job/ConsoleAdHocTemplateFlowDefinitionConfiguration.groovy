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
package org.boozallen.plugins.jte.job

import hudson.Extension
import org.boozallen.plugins.jte.init.governance.config.ConsoleDefaultPipelineTemplate
import org.boozallen.plugins.jte.init.governance.config.ConsolePipelineConfiguration
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor

/**
 * Defines a pipeline configuration repository from the Jenkins console.
 * <p>
 * Allows users to define a pipeline configuration, default pipeline template, and named
 * pipeline templates via a pipeline catalog.
 */
class ConsoleAdHocTemplateFlowDefinitionConfiguration extends AdHocTemplateFlowDefinitionConfiguration{

    private static final long serialVersionUID = 1L
    ConsolePipelineConfiguration pipelineConfig
    ConsoleDefaultPipelineTemplate defaultTemplate

    static ConsoleAdHocTemplateFlowDefinitionConfiguration create(boolean hasTemplate, String template, boolean hasConfig, String config){
        return new ConsoleAdHocTemplateFlowDefinitionConfiguration(
                new ConsoleDefaultPipelineTemplate(hasTemplate, template),
                new ConsolePipelineConfiguration(hasConfig, config)
        )
    }

    @DataBoundConstructor
    ConsoleAdHocTemplateFlowDefinitionConfiguration(
        ConsoleDefaultPipelineTemplate defaultTemplate,
        ConsolePipelineConfiguration pipelineConfig
    ){
        this.defaultTemplate = defaultTemplate
        this.pipelineConfig = pipelineConfig
    }

    ConsolePipelineConfiguration getPipelineConfig(){
        return pipelineConfig
    }

    @Override
    Boolean hasConfig(FlowExecutionOwner flowOwner){
        return pipelineConfig.getProvidePipelineConfig()
    }

    @Override
    PipelineConfigurationObject getConfig(FlowExecutionOwner flowOwner) throws Exception{
        PipelineConfigurationObject conf = null
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        if(pipelineConfig.getProvidePipelineConfig()){
            String pipelineConfigurationString = pipelineConfig.getPipelineConfig()
            try{
                conf = new PipelineConfigurationDsl(flowOwner).parse(pipelineConfigurationString)
            } catch(any){
                logger.printError "Failed to parse pipeline configuration"
                throw any
            }
        }
        return conf
    }

    ConsoleDefaultPipelineTemplate getDefaultTemplate(){
        return defaultTemplate
    }

    @Override
    Boolean hasTemplate(FlowExecutionOwner flowOwner){
        return defaultTemplate.getProvideDefaultTemplate()
    }

    @Override
    String getTemplate(FlowExecutionOwner owner){
        return defaultTemplate.getDefaultTemplate()
    }

    @Extension
    static class DescriptorImpl extends AdHocTemplateFlowDefinitionConfiguration.DescriptorImpl {

        String getDisplayName(){
            return "From Console"
        }

    }

}
