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
import org.boozallen.plugins.jte.init.primitives.NamespaceCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext
import org.boozallen.plugins.jte.util.TemplateLogger

/**
 *  represents a group of library steps to be called.
 */

@SuppressWarnings("NoDef")
class Stage extends TemplatePrimitive implements Serializable{

    private static final long serialVersionUID = 1L
    String name
    ArrayList<String> steps

    Stage(){}

    Stage(String name, ArrayList<String> steps){
        this.name = name
        this.steps = steps
    }

    @NonCPS @Override String getName(){ return name }

    @SuppressWarnings("MethodParameterTypeRequired")
    void call(args) {
        TemplateLogger.createDuringRun().print "[Stage - ${name}]"
        Map stageArgs
        if( args instanceof Object[] && 0 < ((Object[])args).length){
            stageArgs = ((Object[])args)[0]
        } else {
            stageArgs = args as Map
        }
        NamespaceCollector namespaceCollector = NamespaceCollector.current()
        StageContext stageContext = new StageContext(name: name, args: stageArgs)
        steps.each{ step ->
            def clone = namespaceCollector.getStep(step).clone()
            clone.setStageContext(stageContext)
            clone.call()
        }
    }

}
