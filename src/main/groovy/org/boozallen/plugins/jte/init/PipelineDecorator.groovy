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

import hudson.ExtensionList
import hudson.model.InvisibleAction
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.config.ScmPipelineConfigurationProvider
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
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
 * Created from {@link org.boozallen.plugins.jte.job.TemplateFlowDefinition}
 * <p>
 * Action consumed by {@link GroovyShellDecoratorImpl} to attach the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding} to template execution
 */
class PipelineDecorator extends InvisibleAction {

    FlowExecutionOwner flowOwner
    PipelineConfigurationObject config
    TemplateBinding binding
    String template

    PipelineDecorator(FlowExecutionOwner flowOwner) {
        this.flowOwner = flowOwner
    }

    void initialize(){
        config = aggregatePipelineConfigurations()
        binding = injectPrimitives()
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
            String jobConfigString = flowDefinition.getPipelineConfig()
            if(jobConfigString){
                try{
                    jobConfig = new PipelineConfigurationDsl(flowOwner).parse(jobConfigString)
                } catch(any){
                    getLogger().printError("Error parsing ${job.getName()}'s configuration file.")
                    throw any
                }
            }
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

    TemplateBinding injectPrimitives(){
        TemplateBinding templateBinding = new TemplateBinding(flowOwner)
        ExtensionList<TemplatePrimitiveInjector> injectors = TemplatePrimitiveInjector.all()

        injectors.each{ injector ->
            injector.doInject(flowOwner, config, templateBinding)
        }

        injectors.each{ injector ->
            injector.doPostInject(flowOwner, config, templateBinding)
        }

        templateBinding.lock()
        return templateBinding
    }

    String determinePipelineTemplate(){
        LinkedHashMap pipelineConfig = config.getConfig()
        WorkflowJob job = getJob()
        FlowDefinition flowDefinition = job.getDefinition()
        if (flowDefinition instanceof AdHocTemplateFlowDefinition){
            String template = flowDefinition.getTemplate()
            if(template){
                getLogger().print "Obtained Pipeline Template from job configuration"
                return template
            }
        } else {
            FileSystemWrapper fs = FileSystemWrapper.createFromJob(flowOwner)
            String repoJenkinsfile = fs.getFileContents("Jenkinsfile", "Repository Jenkinsfile", false)
            if (repoJenkinsfile){
                Boolean allowScmJenkinsfile = pipelineConfig.containsKey("allow_scm_jenkinsfile") ? pipelineConfig.allow_scm_jenkinsfile : true
                if (allowScmJenkinsfile){
                    return repoJenkinsfile
                }
                getLogger().printWarning "Repository provided Jenkinsfile that will not be used, per organizational policy."
            }
        }

        // specified pipeline template from pipeline template directories in governance tiers
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)
        if (pipelineConfig.pipeline_template){
            for (tier in tiers){
                String pipelineTemplate = tier.getTemplate(flowOwner, pipelineConfig.pipeline_template)
                if (pipelineTemplate){
                    return pipelineTemplate
                }
            }
            throw new Exception("Pipeline Template ${pipelineConfig.pipeline_template} could not be found in hierarchy.")
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

}
