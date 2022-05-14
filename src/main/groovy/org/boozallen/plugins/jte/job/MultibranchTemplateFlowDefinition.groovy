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
import org.boozallen.plugins.jte.init.governance.config.ScmPipelineConfigurationProvider
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.FileSystemWrapperFactory
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundSetter

/**
 * Allows JTE to be used in a MultiBranch Pipeline
 */
class MultibranchTemplateFlowDefinition extends TemplateFlowDefinition {

    String scriptPath
    String configurationPath

    Object readResolve() {
        if (this.scriptPath == null) {
            this.scriptPath = 'Jenkinsfile'
        }
        if (this.configurationPath == null) {
            this.configurationPath = ScmPipelineConfigurationProvider.CONFIG_FILE
        }
        return this
    }

    @DataBoundSetter
    void setScriptPath(String scriptPath){
        this.scriptPath = Util.fixEmptyAndTrim(scriptPath) ?: 'Jenkinsfile'
    }

    String getScriptPath(){
        return scriptPath
    }

    @DataBoundSetter
    void setConfigurationPath(String configurationPath){
        this.configurationPath = Util.fixEmptyAndTrim(configurationPath) ?: ScmPipelineConfigurationProvider.CONFIG_FILE
    }

    String getConfigurationPath(){
        return configurationPath
    }

    @Override
    PipelineConfigurationObject getPipelineConfiguration(FlowExecutionOwner flowOwner) {
        PipelineConfigurationObject jobConfig = null
        FileSystemWrapper fsw = FileSystemWrapperFactory.create(flowOwner)
        String repoConfigFile = fsw.getFileContents(configurationPath, "Template Configuration File", false)
        if (repoConfigFile){
            try{
                jobConfig = new PipelineConfigurationDsl(flowOwner).parse(repoConfigFile)
            } catch(any){
                TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
                logger.printError("Error parsing the pipeline configuration file in SCM.")
                throw any
            }
        }
        return jobConfig
    }

    @Override
    String getTemplate(FlowExecutionOwner flowOwner){
        FileSystemWrapper fs = FileSystemWrapperFactory.create(flowOwner)
        String template = fs.getFileContents(this.scriptPath, "Repository Jenkinsfile", false)
        return template
    }

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
                return context instanceof WorkflowJob && ((WorkflowJob) context).getParent() instanceof WorkflowMultiBranchProject
            }
            return true
        }
    }

}
