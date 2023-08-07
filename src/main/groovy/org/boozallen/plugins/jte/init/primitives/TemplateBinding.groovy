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
package org.boozallen.plugins.jte.init.primitives

import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.DSL
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * A custom binding that prevents users from inadvertently
 * overriding TemplatePrimitives or ReservedVariables
 */
class TemplateBinding extends Binding{

    @Override
    void setVariable(String name, Object value){
        checkPrimitiveCollision(name)
        checkReservedVariables(name)
        super.setVariable(name, value)
    }

    @Override
    Object getVariable(String name) {
        // this needs to match CpsScript.STEPS_VAR, which is private
        if (name == "steps"){
            CpsThread thread = CpsThread.current()
            FlowExecutionOwner flowOwner = thread.getExecution().getOwner()
            return new DSL(flowOwner)
        }
        return super.getVariable(name)
    }

    void checkPrimitiveCollision(String name){
        TemplatePrimitiveCollector collector = TemplatePrimitiveCollector.currentNoException()
        // build may not have started yet in the case of CpsScript.$initialize()
        if(collector == null){
            return
        }
        List<TemplatePrimitive> collisions = collector.findAll{ TemplatePrimitive primitive ->
            primitive.getName() == name
        }
        if(!collisions.isEmpty()){
            TemplateLogger logger = TemplateLogger.createDuringRun()
            logger.printError "Failed to set variable '${name}'."
            if(collisions.size() == 1){
                logger.printError "This would override the ${collisions.first().toString()}"
            } else {
                logger.printError "This would override the following JTE primitives: "
                collisions.eachWithIndex { TemplatePrimitive primitive, int idx ->
                    logger.printError "  ${idx + 1}. ${primitive.toString()}"
                }
            }
            throw new JTEException("Binding collision with JTE template primitive")
        }
    }

    void checkReservedVariables(String name){
        ReservedVariableName reservedVar = ReservedVariableName.byName(name)
        if(reservedVar != null){
            throw new JTEException("Failed to set variable '${name}'. This is a reserved variable name in the JTE framework.")
        }
    }

}
