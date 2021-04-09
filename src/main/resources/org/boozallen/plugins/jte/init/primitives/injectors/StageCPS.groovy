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

import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.InheritConstructors
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger

/**
 *  represents a group of library steps to be called.
 */

@SuppressWarnings("NoDef")
@InheritConstructors
class StageCPS extends Stage{

    @SuppressWarnings("MethodParameterTypeRequired")
    void call(args) {
        TemplateLogger.createDuringRun().print "[Stage - ${getName()}]"
        Map stageArgs
        if( args instanceof Object[] && 0 < ((Object[])args).length){
            stageArgs = ((Object[])args)[0]
        } else {
            stageArgs = args as Map
        }
        StageContext stageContext = new StageContext(name: getName(), args: stageArgs)
        getSteps().each{ stepName ->
            List<StepWrapper> s = this.getStepWrappers(stepName)
            if(s.size() > 1){
                throw new JTEException("Found more than one step for '${stepName}'")
            }
            StepWrapper clone = s.first().clone()
            clone.setStageContext(stageContext)
            clone.getValue().call()
        }
    }

    @NonCPS
    List<StepWrapper> getStepWrappers(String stepName){
        TemplatePrimitiveCollector primitiveCollector = TemplatePrimitiveCollector.current()
        return primitiveCollector.getSteps(stepName)
    }

}
