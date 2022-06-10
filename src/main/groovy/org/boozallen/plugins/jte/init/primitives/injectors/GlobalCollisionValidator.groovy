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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.steps.StepDescriptor

/**
 * checks for collisions between TemplatePrimitives and Jenkins global variables and steps
 */
@Extension class GlobalCollisionValidator extends TemplatePrimitiveInjector{

    @Override
    void validatePrimitives(CpsFlowExecution exec, PipelineConfigurationObject config, TemplatePrimitiveCollector collector) {
        FlowExecutionOwner flowOwner = exec.getOwner()
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        Map<String, List<TemplatePrimitive>> primitivesByName = [:]
        collector.getPrimitives().each{ primitive ->
            String name = primitive.getName()
            if(!primitivesByName.containsKey(name)){
                primitivesByName[name] = []
            }
            primitivesByName[name] << primitive
        }

        checkForPrimitiveCollisions(primitivesByName, config, logger)
        checkForGlobalVariableCollisions(primitivesByName, flowOwner, logger)
        checkForJenkinsStepCollisions(primitivesByName, logger)
    }

    void checkForPrimitiveCollisions(Map<String, List<TemplatePrimitive>> primitivesByName, PipelineConfigurationObject config, TemplateLogger logger){
        Map primitiveCollisions = primitivesByName.findAll{ key, value -> value.size() > 1 }
        boolean dontAllowDuplicates = !config.getJteBlockWrapper().permissive_initialization
        if(primitiveCollisions){
            primitiveCollisions.each{ name, primitives ->
                primitives.each{ TemplatePrimitive p -> p.setOverloaded(primitives) }
                if(dontAllowDuplicates) {
                    logger.printError("There are multiple primitives with the name '${name}'")
                    primitives.eachWithIndex { primitive, idx ->
                        logger.printError("  ${idx + 1}. ${primitive.toString()}")
                    }
                }
            }
            if(dontAllowDuplicates) {
                throw new JTEException("Overlapping template primitives for names: ${primitiveCollisions.keySet()}")
            }
        }
    }

    void checkForGlobalVariableCollisions(Map<String, List<TemplatePrimitive>> primitivesByName, FlowExecutionOwner flowOwner, TemplateLogger logger){
        List<String> msg = ["Template Primitives are colliding with Global Variables: "]
        primitivesByName.keySet().each{ String name ->
            List<GlobalVariable> vars
            vars = TemplatePrimitiveCollector.getGlobalVariablesByName(name, flowOwner.run())
            vars = vars.findAll{ variable -> !(variable in TemplatePrimitive) }
            if(vars){
                vars.eachWithIndex{ variable, idx ->
                    msg << "  ${idx + 1}. ${name}: ${variable}"
                }
                logger.printWarning(msg.join("\n"))
            }
        }
    }

    void checkForJenkinsStepCollisions(Map<String, List<TemplatePrimitive>> primitivesByName, TemplateLogger logger){
        List<String> functionNames = StepDescriptor.all().collect { step ->
            step.getFunctionName()
        }
        List<String> stepCollisions = primitivesByName.keySet().intersect(functionNames) as List
        if(!stepCollisions.isEmpty()) {
            List<String> msg = ["Template Primitives are overwriting Jenkins steps with the following names:"]
            stepCollisions.eachWithIndex { step, idx ->
                msg << "  ${idx + 1}. ${step}"
            }
            logger.printWarning(msg.join("\n"))
        }
    }

}
