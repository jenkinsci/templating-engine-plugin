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
import hudson.init.InitMilestone
import hudson.init.Initializer
import hudson.model.Descriptor
import hudson.model.DescriptorVisibilityFilter
import hudson.model.Items
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor

/**
 * Allows JTE to be used in a Pipeline Job
 */
class AdHocTemplateFlowDefinition extends TemplateFlowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient boolean providePipelineTemplate
    private transient String template
    private transient boolean providePipelineConfig
    private transient String pipelineConfig

    AdHocTemplateFlowDefinitionConfiguration configProvider

    @DataBoundConstructor
    AdHocTemplateFlowDefinition(AdHocTemplateFlowDefinitionConfiguration configProvider){
        this.configProvider = configProvider
    }

    AdHocTemplateFlowDefinitionConfiguration getConfigProvider(){
        return configProvider
    }

    PipelineConfigurationObject getPipelineConfiguration(FlowExecutionOwner flowOwner){
        return configProvider.hasConfig(flowOwner) ? configProvider.getConfig(flowOwner) : null
    }

    String getTemplate(FlowExecutionOwner flowOwner){
        return configProvider.hasTemplate(flowOwner) ? configProvider.getTemplate(flowOwner) : null
    }

    protected Object readResolve(){
        if( configProvider == null ){
            this.configProvider = ConsoleAdHocTemplateFlowDefinitionConfiguration.create(
                this.providePipelineTemplate, this.template, this.providePipelineConfig, this.pipelineConfig
            )
        }

        return this
    }

    @Extension
    static class DescriptorImpl extends FlowDefinitionDescriptor {
        @Initializer(before = InitMilestone.PLUGINS_STARTED)
        static void addAliases() {
            Items.XSTREAM2.addCompatibilityAlias("org.boozallen.plugins.jte.job.TemplateFlowDefinition", org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition)
        }

        @Override
        String getDisplayName() {
            return "Jenkins Templating Engine"
        }

        Descriptor getDefaultConfigurationProvider(){
            return Jenkins.get().getDescriptor(ConsoleAdHocTemplateFlowDefinitionConfiguration)
        }
    }

    @Extension
    static class HideMeElsewhere extends DescriptorVisibilityFilter {

        @Override
        boolean filter(Object context, Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl) {
                return context instanceof WorkflowJob && !(((WorkflowJob) context).getParent() instanceof WorkflowMultiBranchProject)
            }
            return true
        }

    }

}
