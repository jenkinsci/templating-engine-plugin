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
package org.boozallen.plugins.jte.init.governance.config

import hudson.Extension
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor

/**
 * Defines a pipeline configuration repository from the Jenkins console.
 * <p>
 * Allows users to define a pipeline configuration, default pipeline template, and named
 * pipeline templates via a pipeline catalog.
 */
class ConsolePipelineConfigurationProvider extends PipelineConfigurationProvider{

    ConsolePipelineConfiguration pipelineConfig
    ConsoleDefaultPipelineTemplate defaultTemplate
    List<ConsoleNamedPipelineTemplate> pipelineCatalog

    @DataBoundConstructor
    ConsolePipelineConfigurationProvider(
        ConsoleDefaultPipelineTemplate defaultTemplate,
        ConsolePipelineConfiguration pipelineConfig,
        List<ConsoleNamedPipelineTemplate> pipelineCatalog
    ){
        this.defaultTemplate = defaultTemplate
        this.pipelineConfig = pipelineConfig
        this.pipelineCatalog = pipelineCatalog
    }

    ConsolePipelineConfiguration getPipelineConfig(){
        return pipelineConfig
    }

    ConsoleDefaultPipelineTemplate getDefaultTemplate(){
        return defaultTemplate
    }

    List<ConsoleNamedPipelineTemplate> getPipelineCatalog(){
        return pipelineCatalog
    }

    @Override
    PipelineConfigurationObject getConfig(FlowExecutionOwner owner){
        return  pipelineConfig.getProvidePipelineConfig() ?
                new PipelineConfigurationDsl(owner).parse(pipelineConfig.getPipelineConfig()) :
                null
    }

    @Override
    String getJenkinsfile(FlowExecutionOwner owner){
        return defaultTemplate.getDefaultTemplate()
    }

    @Override
    String getTemplate(FlowExecutionOwner owner, String templateName){
        ConsoleNamedPipelineTemplate template = pipelineCatalog.find{ item -> item.getName() == templateName }
        return template ? template.getTemplate() : null
    }

    @Extension
    static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{

        String getDisplayName(){
            return "From Console"
        }

    }

}
