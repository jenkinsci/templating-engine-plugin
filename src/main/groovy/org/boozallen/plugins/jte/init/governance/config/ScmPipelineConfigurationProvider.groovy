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
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

class ScmPipelineConfigurationProvider extends PipelineConfigurationProvider{

    static final String CONFIG_FILE = "pipeline_config.groovy"
    static final String PIPELINE_TEMPLATE_DIRECTORY = "pipeline_templates"
    String baseDir
    SCM scm

    @DataBoundConstructor
    ScmPipelineConfigurationProvider(){}

    @DataBoundSetter
    setBaseDir(String baseDir){
        this.baseDir = Util.fixEmptyAndTrim(baseDir)
    }

    String getBaseDir(){ return baseDir }

    @DataBoundSetter
    setScm(SCM scm){ this.scm = scm }

    SCM getScm(){ return scm }

    PipelineConfigurationObject getConfig(FlowExecutionOwner owner){
        PipelineConfigurationObject configObject
        if (scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(owner, scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${CONFIG_FILE}"
            String configFile = fsw.getFileContents(filePath, "Pipeline Configuration File")
            if (configFile){
                try{
                    configObject = new PipelineConfigurationDsl(owner).parse(configFile)
                }catch(any){
                    new TemplateLogger(owner.getListener()).printError("Error parsing scm provided pipeline configuration")
                    throw any
                }
            }
        }
        return configObject
    }

    String getJenkinsfile(FlowExecutionOwner owner){
        String jenkinsfile
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(owner, scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}Jenkinsfile"
            jenkinsfile = fsw.getFileContents(filePath, "Template")
        }
        return jenkinsfile
    }

    String getTemplate(FlowExecutionOwner owner, String template){
        String pipelineTemplate
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(owner, scm)
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
