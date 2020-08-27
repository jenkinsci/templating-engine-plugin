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
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor

/**
 * A null configuration provider when not defining a pipeline configuration on a
 * {@link org.boozallen.plugins.jte.init.governance.GovernanceTier}
 */
@SuppressWarnings(['UnnecessaryConstructor'])
class NullPipelineConfigurationProvider extends PipelineConfigurationProvider{

    @DataBoundConstructor NullPipelineConfigurationProvider(){}
    @Override PipelineConfigurationObject getConfig(FlowExecutionOwner owner){ return null }
    @Override String getJenkinsfile(FlowExecutionOwner owner){ return null }
    @Override String getTemplate(FlowExecutionOwner owner, String template){ return null }

    @Extension
    static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        String getDisplayName(){
            return "None"
        }
    }

}
