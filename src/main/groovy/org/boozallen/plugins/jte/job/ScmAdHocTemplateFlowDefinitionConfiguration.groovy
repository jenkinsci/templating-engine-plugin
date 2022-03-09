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
import hudson.scm.NullSCM
import hudson.scm.SCM
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.FileSystemWrapperFactory
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor

/**
 * Defines a pipeline configuration repository from the Jenkins console.
 * <p>
 * Allows users to define a pipeline configuration, default pipeline template, and named
 * pipeline templates via a pipeline catalog.
 */
class ScmAdHocTemplateFlowDefinitionConfiguration extends AdHocTemplateFlowDefinitionConfiguration{

    SCM scm
    String pipelineConfigurationPath
    String pipelineTemplatePath

    @DataBoundConstructor
    ScmAdHocTemplateFlowDefinitionConfiguration(SCM scm, String pipelineConfigurationPath, String pipelineTemplatePath){
        this.scm = scm
        this.pipelineConfigurationPath = Util.fixEmptyAndTrim(pipelineConfigurationPath)
        this.pipelineTemplatePath = Util.fixEmptyAndTrim(pipelineTemplatePath)
    }

    @Override
    Boolean hasConfig(FlowExecutionOwner flowOwner){
        return pipelineConfigurationPath // && null != getConfig(flowOwner)
    }

    @Override
    PipelineConfigurationObject getConfig(FlowExecutionOwner flowOwner) throws Exception{
        PipelineConfigurationObject configObject = null
        if (scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapperFactory.create(flowOwner, scm)
            String configFile = fsw.getFileContents(pipelineConfigurationPath, "Pipeline Configuration File")
            if (configFile){
                try{
                    configObject = new PipelineConfigurationDsl(flowOwner).parse(configFile)
                } catch(any){
                    new TemplateLogger(flowOwner.getListener()).printError("Error parsing scm provided pipeline configuration")
                    throw any
                }
            }
        }
        return configObject
    }

    @Override
    Boolean hasTemplate(FlowExecutionOwner flowOwner){
        return this.pipelineTemplatePath // && null != getTemplate(flowOwner)
    }

    @Override
    String getTemplate(FlowExecutionOwner flowOwner){
        String jenkinsfile = null
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapperFactory.create(flowOwner, scm)
            jenkinsfile = fsw.getFileContents(this.pipelineTemplatePath, "Template")
        }
        return jenkinsfile
    }

    @Extension
    static class DescriptorImpl extends AdHocTemplateFlowDefinitionConfiguration.DescriptorImpl {

        String getDisplayName(){
            return "From SCM"
        }

    }

}
