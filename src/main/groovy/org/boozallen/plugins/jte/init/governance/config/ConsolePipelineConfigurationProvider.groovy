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
import hudson.Util
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor

class ConsolePipelineConfigurationProvider extends PipelineConfigurationProvider{

    boolean providePipelineConfig
    String pipelineConfig
    boolean provideDefaultTemplate
    String defaultTemplate
    List<ConsolePipelineTemplate> pipelineCatalog

    @DataBoundConstructor
    ConsolePipelineConfigurationProvider(boolean providePipelineConfig, String pipelineConfig,
                                         boolean provideDefaultTemplate, String defaultTemplate,
                                         List<ConsolePipelineTemplate> pipelineCatalog){
        this.providePipelineConfig = providePipelineConfig
        this.pipelineConfig = providePipelineConfig ? Util.fixEmptyAndTrim(pipelineConfig) : null
        this.provideDefaultTemplate = provideDefaultTemplate
        this.defaultTemplate = provideDefaultTemplate ? Util.fixEmptyAndTrim(defaultTemplate) : null
        this.pipelineCatalog = pipelineCatalog
    }

    boolean getProvidePipelineConfig(){
        return providePipelineConfig
    }

    String getPipelineConfig(){
        return pipelineConfig
    }

    boolean getProvideDefaultTemplate(){
        return provideDefaultTemplate
    }

    String getDefaultTemplate(){
        return defaultTemplate
    }

    List<ConsolePipelineTemplate> getPipelineCatalog(){
        return pipelineCatalog
    }

    @Override
    PipelineConfigurationObject getConfig(FlowExecutionOwner owner){
        return pipelineConfig ? new PipelineConfigurationDsl(owner).parse(pipelineConfig) : null
    }

    @Override
    String getJenkinsfile(FlowExecutionOwner owner){
        return defaultTemplate
    }

    @Override
    String getTemplate(FlowExecutionOwner owner, String templateName){
        ConsolePipelineTemplate template = pipelineCatalog.find{ item -> item.getName() == templateName }
        return template ? template.getTemplate() : null
    }

    @Extension
    static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{

        String getDisplayName(){
            return "From Console"
        }

    }

}
