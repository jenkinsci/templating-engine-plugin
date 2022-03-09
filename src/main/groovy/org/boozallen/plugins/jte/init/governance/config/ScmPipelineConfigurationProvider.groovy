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
import hudson.scm.NullSCM
import hudson.scm.SCM
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.FileSystemWrapperFactory
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

/**
 * Fetches pipeline configuration from a source code repository
 */
class ScmPipelineConfigurationProvider extends PipelineConfigurationProvider{

    static final String CONFIG_FILE = "pipeline_config.groovy"
    static final String PIPELINE_TEMPLATE_DIRECTORY = "pipeline_templates"
    String baseDir
    SCM scm

    // jenkins requires this be here
    @SuppressWarnings('UnnecessaryConstructor')
    @DataBoundConstructor
    ScmPipelineConfigurationProvider(){}

    @DataBoundSetter
    void setBaseDir(String _baseDir){
        this.baseDir = Util.fixEmptyAndTrim(_baseDir)
    }

    String getBaseDir(){ return baseDir }

    @DataBoundSetter
    void setScm(SCM _scm){ this.scm = _scm }

    SCM getScm(){ return scm }

    @Override
    PipelineConfigurationObject getConfig(FlowExecutionOwner owner){
        PipelineConfigurationObject configObject = null
        if (scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapperFactory.create(owner, scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${CONFIG_FILE}"
            String configFile = fsw.getFileContents(filePath, "Pipeline Configuration File")
            if (configFile){
                try{
                    configObject = new PipelineConfigurationDsl(owner).parse(configFile)
                } catch(any){
                    new TemplateLogger(owner.getListener()).printError("Error parsing scm provided pipeline configuration")
                    throw any
                }
            }
        }
        return configObject
    }

    @Override
    String getJenkinsfile(FlowExecutionOwner owner){
        String jenkinsfile = null
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapperFactory.create(owner, scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}Jenkinsfile"
            jenkinsfile = fsw.getFileContents(filePath, "Template")
        }
        return jenkinsfile
    }

    @Override
    String getTemplate(FlowExecutionOwner owner, String template){
        String pipelineTemplate = null
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapperFactory.create(owner, scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${PIPELINE_TEMPLATE_DIRECTORY}/${template}"
            pipelineTemplate = fsw.getFileContents(filePath, "Pipeline Template")
        }
        return pipelineTemplate
    }

    @Extension
    static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        String getDisplayName(){
            return "From SCM"
        }
    }

}
