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

import org.boozallen.plugins.jte.binding.*
import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.utils.RunUtils
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.console.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 
import jenkins.model.Jenkins


@Extension class StageInjector extends TemplatePrimitiveInjector {
    static void doInject(TemplateConfigObject config, CpsScript script){
        Class Stage = getPrimitiveClass()
        config.getConfig().stages.each{name, steps ->
            ArrayList<String> stepsList = new ArrayList()
            steps.collect(stepsList){ it.key }
            script.getBinding().setVariable(name, Stage.newInstance(script, name, stepsList))
        }
    }

    static Class getPrimitiveClass(){
        String self = "org.boozallen.plugins.jte.binding.injectors.StageInjector"
        String classText = RunUtils.classLoader
                                    .loadClass(self)
                                    .getResource("Stage.groovy")
                                    .text
        return TemplateScriptEngine.parseClass(classText)
    }
}