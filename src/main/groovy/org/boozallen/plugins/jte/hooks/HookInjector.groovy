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

package org.boozallen.plugins.jte.hooks

import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.utils.RunUtils
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.binding.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 
import jenkins.model.Jenkins

/*
    Hooks invoke other pipeline code so the class that gets called needs to be CPS 
    transformed.  This injector just gets used as a hook to insert the Hooks and 
    AnnotatedMethod class into the ClassLoader after being CPS Transformed via 
    TemplateScriptEngine.parseClass
*/
@Extension class HookInjector extends TemplatePrimitiveInjector {

    static void doInject(TemplateConfigObject config, CpsScript script){
        getHooksClass()
        getAnnotatedMethodClass()
    }

    static Class getHooksClass(){
        String self = "org.boozallen.plugins.jte.hooks.HookInjector"
        String classText = RunUtils.classLoader
                                    .loadClass(self)
                                    .getResource("Hooks.groovy")
                                    .text
        return TemplateScriptEngine.parseClass(classText)
    }

    static Class getAnnotatedMethodClass(){
        String self = "org.boozallen.plugins.jte.hooks.HookInjector"
        String classText = RunUtils.classLoader
                                    .loadClass(self)
                                    .getResource("AnnotatedMethod.groovy")
                                    .text
        return TemplateScriptEngine.parseClass(classText)
    }
}

