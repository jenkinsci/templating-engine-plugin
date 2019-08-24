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

package org.boozallen.plugins.jte.binding.injectors

import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.binding.*
import org.boozallen.plugins.jte.console.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 
import jenkins.model.Jenkins
import org.codehaus.groovy.runtime.InvokerHelper

/*
    represents a group of library steps to be called. 
*/
class Stage extends TemplatePrimitive implements Serializable{
    CpsScript script 
    String name
    ArrayList<String> steps

    Stage(){}

    Stage(CpsScript script, String name, ArrayList<String> steps){
        this.script = script
        this.name = name
        this.steps = steps 
    }

    void call(Object... args){
        TemplateLogger.print "[Stage - ${name}]" 
        for(def i = 0; i < steps.size(); i++){
            String step = steps.get(i)
            InvokerHelper.getMetaClass(script).invokeMethod(script, step, args)
        }
    }

    void throwPreLockException(){
        throw new TemplateException ("The Stage ${name} is already defined.")
    }

    void throwPostLockException(){
        throw new TemplateException ("The variable ${name} is reserved as a template Stage.")
    }

}