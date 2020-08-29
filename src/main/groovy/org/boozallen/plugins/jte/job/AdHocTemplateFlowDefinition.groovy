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
import hudson.Util
import hudson.model.Descriptor
import hudson.model.DescriptorVisibilityFilter
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor

/**
 * Allows JTE to be used in a Pipeline Job
 */
class AdHocTemplateFlowDefinition extends TemplateFlowDefinition {

    private final boolean providePipelineTemplate
    private final String template
    private final boolean providePipelineConfig
    private final String pipelineConfig

    @DataBoundConstructor
    AdHocTemplateFlowDefinition(boolean providePipelineTemplate, String template, boolean providePipelineConfig, String pipelineConfig){
        this.providePipelineTemplate = providePipelineTemplate
        this.template = providePipelineTemplate ? Util.fixEmptyAndTrim(template) : null
        this.providePipelineConfig = providePipelineConfig
        this.pipelineConfig = providePipelineConfig ? Util.fixEmptyAndTrim(pipelineConfig) : null
    }

    boolean getProvidePipelineTemplate(){ return providePipelineTemplate }

    String getTemplate() { return template }

    boolean getProvidePipelineConfig(){ return providePipelineConfig }

    String getPipelineConfig(){ return pipelineConfig }

    @Extension
    static class DescriptorImpl extends FlowDefinitionDescriptor {
        @Override
        String getDisplayName() {
            return "Jenkins Templating Engine"
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
