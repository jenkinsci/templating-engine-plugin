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
import hudson.model.Run
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.NamespaceCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.steps.StepDescriptor

/**
 * checks for collisions between TemplatePrimitives and Jenkins global variables and steps
 */
@Extension class GlobalCollisionValidator extends TemplatePrimitiveInjector{

    static String warningHeading = "JTE Primitives overrode Plugin provided steps and/or variables:"

    @Override
    void validatePrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config) {
        WorkflowRun run = flowOwner.run()
        if(!run) return
        Set<String> collisions = checkPrimitiveCollisions(run)

        if( collisions ){
            List<String> warnings = [ warningHeading ]
            collisions.each{ name ->
                warnings << "- ${name}"
            }
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printWarning(warnings.join("\n"))
        }
    }

    // will probably become a method on the validation class
    Set<String> checkPrimitiveCollisions(Run run){
        Set<String> collisions = []
        NamespaceCollector namespaceCollector = run.getAction(NamespaceCollector)
        if(!namespaceCollector) return collisions
        Set<String> registry = namespaceCollector.getPrimitiveNames()
        List<String> functionNames = StepDescriptor.all()*.functionName
        collisions = registry.intersect(functionNames)

        // FIXME: now that we're using GV's for primitives, this changes
        collisions += registry.collect { key ->
            GlobalVariable.byName(key, run)
        }.findAll{ g -> null != g }

        return new ArrayList<String>(collisions as Collection<String>)
    }

}
