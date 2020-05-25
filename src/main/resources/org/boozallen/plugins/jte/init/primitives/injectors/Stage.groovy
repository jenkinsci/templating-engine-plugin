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
import org.boozallen.plugins.jte.init.primitives.TemplateException
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper

/*
    represents a group of library steps to be called. 
*/
class Stage extends TemplatePrimitive implements Serializable{

    static EMPTY_CONTEXT = [ name: null, args: [:] ]

    Binding binding 
    String name
    ArrayList<String> steps

    Stage(){}

    Stage(Binding binding, String name, ArrayList<String> steps){
        this.binding = binding
        this.name = name
        this.steps = steps 
    }

    void call(stageConfig){
        TemplateLogger.createDuringRun().print "[Stage - ${name}]"

        def stageContext = [
            name: name,
            args: stageConfig
        ]

        for(def i = 0; i < steps.size(); i++){
            String step = steps.get(i)
            setStageContext(step, stageContext)
            binding.getStep(step).call()
            setStageContext(step, EMPTY_CONTEXT)
        }
    }

    @NonCPS
    private void setStageContext(String step, stageContext) {
        if(!binding.hasStep(step)) {
            return
        }
        def impl = binding.getStep(step)?.impl
        def metaClass = InvokerHelper.getMetaClass(impl)
        metaClass.getStageContext = {-> stageContext}
    }

    void throwPreLockException(){
        throw new TemplateException ("The Stage ${name} is already defined.")
    }

    void throwPostLockException(){
        throw new TemplateException ("The variable ${name} is reserved as a template Stage.")
    }

}