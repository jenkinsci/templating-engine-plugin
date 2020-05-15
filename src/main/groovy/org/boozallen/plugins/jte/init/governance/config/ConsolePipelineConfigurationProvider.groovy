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
import hudson.RelativePath
import hudson.Util
import hudson.util.FormValidation
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

public class ConsolePipelineConfigurationProvider extends PipelineConfigurationProvider{

    boolean providePipelineConfig
    String pipelineConfig
    boolean provideDefaultTemplate
    String defaultTemplate
    List<ConsolePipelineTemplate> pipelineCatalog

    @DataBoundConstructor
    public ConsolePipelineConfigurationProvider(boolean providePipelineConfig, String pipelineConfig,
    boolean provideDefaultTemplate, String defaultTemplate,
    List<ConsolePipelineTemplate> pipelineCatalog){
        this.providePipelineConfig = providePipelineConfig
        this.pipelineConfig = providePipelineConfig ? Util.fixEmptyAndTrim(pipelineConfig) : null
        this.provideDefaultTemplate = provideDefaultTemplate
        this.defaultTemplate = provideDefaultTemplate ? Util.fixEmptyAndTrim(defaultTemplate) : null
        this.pipelineCatalog = pipelineCatalog
    }

    public boolean getProvidePipelineConfig(){
        return providePipelineConfig
    }
    public String getPipelineConfig(){
        return pipelineConfig
    }
    public boolean getProvideDefaultTemplate(){
        return provideDefaultTemplate
    }
    public String getDefaultTemplate(){
        return defaultTemplate
    }
    public List<ConsolePipelineTemplate> getPipelineCatalog(){
        return pipelineCatalog
    }

    public PipelineConfigurationObject getConfig(FlowExecutionOwner owner){
        return pipelineConfig ? new PipelineConfigurationDsl(owner).parse(pipelineConfig) : null
    }

    public String getJenkinsfile(FlowExecutionOwner owner){
        return defaultTemplate
    }

    public String getTemplate(FlowExecutionOwner owner, String templateName){
        ConsolePipelineTemplate template = pipelineCatalog.find{ it.getName() == templateName }
        return template ? template.getTemplate() : null
    }

    @Extension public static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        public String getDisplayName(){
            return "From Console"
        }
    }
}
