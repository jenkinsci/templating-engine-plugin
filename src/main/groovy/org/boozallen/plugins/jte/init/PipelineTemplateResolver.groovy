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

import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Determines the Pipeline Template for a given Run
 */
class PipelineTemplateResolver {

    FlowExecutionOwner flowOwner
    WorkflowJob job

    PipelineTemplateResolver(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
        this.job = flowOwner.run().getParent()
    }

    String resolve(PipelineConfigurationObject config){
        TemplateFlowDefinition flowDefinition = job.getDefinition()
        JteBlockWrapper jteBlockWrapper = config.jteBlockWrapper
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        // check for job-level template
        String template = flowDefinition.getTemplate(flowOwner)
        if (template){
            // templates configured in Jenkins UI are always permitted.
            if (flowDefinition instanceof AdHocTemplateFlowDefinition){
                logger.print "Obtained Pipeline Template from job configuration"
                return template
            }
            // templates from SCM depend on Pipeline Configuration
            if (jteBlockWrapper.allow_scm_jenkinsfile){
                return template
            }
            logger.printWarning "Repository provided Jenkinsfile that will not be used, per organizational policy."
        }

        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)
        // if aggregated config declares use of a Named Pipeline Template, look for it:
        if (jteBlockWrapper.pipeline_template){
            for (tier in tiers){
                String pipelineTemplate = tier.getTemplate(flowOwner, jteBlockWrapper.pipeline_template)
                if (pipelineTemplate){
                    return pipelineTemplate
                }
            }
            throw new Exception("Pipeline Template ${jteBlockWrapper.pipeline_template} could not be found in hierarchy.")
        }

        // otherwise, look for a Default Pipeline Template
        for (tier in tiers){
            String pipelineTemplate = tier.getJenkinsfile(flowOwner)
            if (pipelineTemplate){
                return pipelineTemplate
            }
        }

        throw new Exception("Could not determine pipeline template.")
    }

}
