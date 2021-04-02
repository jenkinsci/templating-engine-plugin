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
package org.boozallen.plugins.jte.init

import hudson.model.InvisibleAction
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.config.ScmPipelineConfigurationProvider
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Initializes a JTE pipeline
 * <p>
 * Initialization follows 3 steps:
 * <ol>
 * <li> aggregate pipeline configurations
 * <li> pass the aggregated pipeline configuration to every {@link org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector}
 * <li> determine the pipeline template for this run
 * </ol>
 * <p>
 * note: this is only still extends InvisibleAction and implements Serializable
 * for backwords compatibility to avoid exceptions when trying to load
 * builds on a previous version of the plugin.
 *
 * TODO: remove InvisibleAction extends Serializable when appropriate
 */
class PipelineDecorator extends InvisibleAction implements Serializable{

    private static final long serialVersionUID = 1L
    FlowExecutionOwner flowOwner
    PipelineConfigurationObject config
    String template
    transient TemplateBinding binding

    PipelineDecorator(FlowExecutionOwner flowOwner) {
        this.flowOwner = flowOwner
    }

    void initialize(){
        config   = aggregatePipelineConfigurations()
        TemplatePrimitiveInjector.orchestrate(flowOwner, config)
        template = determinePipelineTemplate()
    }

    PipelineConfigurationObject aggregatePipelineConfigurations(){
        PipelineConfigurationObject pipelineConfig = new PipelineConfigurationObject(flowOwner)
        pipelineConfig.firstConfig = true

        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(getJob())

        //  we get the configs in ascending order of governance
        //  so reverse the list to get the highest precedence first
        tiers.reverse().each{ tier ->
            PipelineConfigurationObject tierConfig = tier.getConfig(flowOwner)
            if (tierConfig){
                pipelineConfig += tierConfig
            }
        }

        // get job level configuration
        PipelineConfigurationObject jobConfig = getJobPipelineConfiguration(getJob())
        if(jobConfig){
            pipelineConfig += jobConfig
        }

        return pipelineConfig
    }

    PipelineConfigurationObject getJobPipelineConfiguration(WorkflowJob job){
        PipelineConfigurationObject jobConfig = null
        FlowDefinition flowDefinition = job.getDefinition()
        if(flowDefinition instanceof AdHocTemplateFlowDefinition){
            jobConfig = flowDefinition.getPipelineConfiguration(flowOwner)
        } else {
            // get job config if present
            FileSystemWrapper fsw = FileSystemWrapper.createFromJob(flowOwner)
            String repoConfigFile = fsw.getFileContents(ScmPipelineConfigurationProvider.CONFIG_FILE, "Template Configuration File", false)
            if (repoConfigFile){
                try{
                    jobConfig = new PipelineConfigurationDsl(flowOwner).parse(repoConfigFile)
                } catch(any){
                    getLogger().printError("Error parsing ${job.getName()}'s configuration file in SCM.")
                    throw any
                }
            }
        }
        return jobConfig
    }

    String determinePipelineTemplate(){
        WorkflowJob job = getJob()
        FlowDefinition flowDefinition = job.getDefinition()
        JteBlockWrapper jteBlockWrapper = config.jteBlockWrapper
        if (flowDefinition instanceof AdHocTemplateFlowDefinition){
            String template = flowDefinition.getTemplate(flowOwner)
            if(template){
                getLogger().print "Obtained Pipeline Template from job configuration"
                return template
            }
        } else {
            FileSystemWrapper fs = FileSystemWrapper.createFromJob(flowOwner)
            String repoJenkinsfile = fs.getFileContents("Jenkinsfile", "Repository Jenkinsfile", false)
            if (repoJenkinsfile){
                if (jteBlockWrapper.allow_scm_jenkinsfile){
                    return repoJenkinsfile
                }
                getLogger().printWarning "Repository provided Jenkinsfile that will not be used, per organizational policy."
            }
        }

        // specified pipeline template from pipeline template directories in governance tiers
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)
        if (jteBlockWrapper.pipeline_template){
            for (tier in tiers){
                String pipelineTemplate = tier.getTemplate(flowOwner, jteBlockWrapper.pipeline_template)
                if (pipelineTemplate){
                    return pipelineTemplate
                }
            }
            throw new Exception("Pipeline Template ${jteBlockWrapper.pipeline_template} could not be found in hierarchy.")
        }

        /*
         look for default Jenkinsfile in ascending order of governance tiers
         */
        for (tier in tiers){
            String pipelineTemplate = tier.getJenkinsfile(flowOwner)
            if (pipelineTemplate){
                return pipelineTemplate
            }
        }

        throw new Exception("Could not determine pipeline template.")
    }

    String getTemplate(){
        return template
    }

    TemplateLogger getLogger(){
        return new TemplateLogger(flowOwner.getListener())
    }

    WorkflowJob getJob(){
        return flowOwner.run().getParent()
    }

    @SuppressWarnings("PropertyName")
    static class JteBlockWrapper{
        /**
         * property name should match the config keys which are snake_case/underscore-separated
         */
        String pipeline_template = null
        Boolean allow_scm_jenkinsfile = true
        Boolean permissive_initialization = false
        Boolean reverse_library_resolution = false

        static LinkedHashMap getSchema(){
            return [
                fields: [
                    optional: [
                        allow_scm_jenkinsfile: Boolean,
                        pipeline_template: String,
                        permissive_initialization: Boolean,
                        reverse_library_resolution: Boolean
                    ]
                ]
            ]
        }
    }

}
