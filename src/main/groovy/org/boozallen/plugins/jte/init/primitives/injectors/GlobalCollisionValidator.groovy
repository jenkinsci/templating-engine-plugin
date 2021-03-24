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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.steps.StepDescriptor

/**
 * checks for collisions between TemplatePrimitives and Jenkins global variables and steps
 */
@Extension class GlobalCollisionValidator extends TemplatePrimitiveInjector{

    static String warningHeading = "JTE Primitives overrode Plugin provided steps and/or variables:"

    @Override
    void validatePrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config) {
        TemplatePrimitiveCollector primitiveCollector = getPrimitiveCollector(flowOwner)

        Map primitivesByName = [:]
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        primitiveCollector.getPrimitives().each{ primitive ->
            String name = primitive.getName()
            if(!primitivesByName.containsKey(name)){
                primitivesByName[name] = []
            }
            primitivesByName[name] << primitive
        }

        // check for collisions amongst the primitives
        Map primitiveCollisions = primitivesByName.findAll{ key, value -> value.size() > 1 }
        boolean dontAllowDuplicates = !config.getJteBlockWrapper().permissive_initialization
        if(primitiveCollisions){
            primitiveCollisions.each{ name, primitives ->
                primitives.each{ TemplatePrimitive p -> p.setOverloaded(primitives) }
                if(dontAllowDuplicates) {
                    logger.printError("There are multiple primitives with the name '${name}'")
                    primitives.each { primitive ->
                        logger.printError("- ${primitive.toString()}")
                    }
                }
            }
            if(dontAllowDuplicates) {
                throw new JTEException("Overlapping template primitives for names: ${primitiveCollisions.keySet()}")
            }
        }
        // TODO: check for collisions with other global variables
        // TODO: check for collisions with Jenkins DSL Steps
    }

    // will probably become a method on the validation class
    Set<String> checkPrimitiveCollisions(Run run){
        Set<String> collisions = []
        TemplatePrimitiveCollector primitiveCollector = run.getAction(TemplatePrimitiveCollector)
        if(!primitiveCollector){
            return collisions
        }
        Set<String> registry = primitiveCollector.getPrimitiveNames()
        List<String> functionNames = StepDescriptor.all()*.functionName
        collisions = registry.intersect(functionNames)

        // FIXME: now that we're using GV's for primitives, this changes
        collisions += registry.collect { key ->
            GlobalVariable.byName(key, run)
        }.findAll{ g -> null != g }

        return new ArrayList<String>(collisions as Collection<String>)
    }

}
