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
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Fetches the Pipeline Configurations from the Configuration Hierarchy and merges them
 * yielding an Aggregated Pipeline Configuration
 */
class PipelineConfigurationAggregator {

    FlowExecutionOwner flowOwner
    WorkflowJob job

    PipelineConfigurationAggregator(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
        this.job = flowOwner.run().getParent()
    }

    PipelineConfigurationObject aggregate(){
        // create the initial PipelineConfigurationObject
        PipelineConfigurationObject pipelineConfig = new PipelineConfigurationObject(flowOwner)
        pipelineConfig.firstConfig = true

        // get the Configuration Hierarchy
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)

        //  we get the configs in ascending order of governance
        //  so reverse the list to get the highest precedence first
        tiers.reverse().each{ tier ->
            PipelineConfigurationObject tierConfig = tier.getConfig(flowOwner)
            if (tierConfig){
                pipelineConfig += tierConfig
            }
        }

        // get job level configuration
        PipelineConfigurationObject jobConfig = getJobPipelineConfiguration()
        if(jobConfig){
            pipelineConfig += jobConfig
        }

        return pipelineConfig
    }

    PipelineConfigurationObject getJobPipelineConfiguration(){
        TemplateFlowDefinition flowDefinition = job.getDefinition()
        return flowDefinition.getPipelineConfiguration(flowOwner)
    }

}
