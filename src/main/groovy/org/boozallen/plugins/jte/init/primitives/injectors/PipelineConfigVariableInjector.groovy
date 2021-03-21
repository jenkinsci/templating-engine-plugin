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
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.Extension
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.NamespaceCollector
import org.boozallen.plugins.jte.init.primitives.NamespaceCollector.PrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

import javax.annotation.Nonnull

/**
 * Exposes the aggregated pipeline configuration as a variable called 'pipelineConfig'
 */
@Extension class PipelineConfigVariableInjector extends TemplatePrimitiveInjector {

    static final String KEY = "pipelineConfig"

    @SuppressWarnings('NoDef')
    @Override
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config){
        PipelineConfigGlobalVariable pipelineConfig = new PipelineConfigGlobalVariable(config.getConfig())
        PrimitiveNamespace pipelineConfigNamespace = NamespaceCollector.createNamespace(KEY)
        pipelineConfigNamespace.add(pipelineConfig)

        // add the namespace to the collector and save it on the run
        NamespaceCollector namespaceCollector = getNamespaceCollector(flowOwner)
        namespaceCollector.addNamespace(pipelineConfigNamespace)
        flowOwner.run().addOrReplaceAction(namespaceCollector)
    }


    class PipelineConfigGlobalVariable extends GlobalVariable{
        Map config

        PipelineConfigGlobalVariable(Map config){
            this.config = config
        }

        String getName(){
            return KEY
        }

        @Override
        Object getValue(@Nonnull CpsScript script) throws Exception {
            return config
        }
    }

}
