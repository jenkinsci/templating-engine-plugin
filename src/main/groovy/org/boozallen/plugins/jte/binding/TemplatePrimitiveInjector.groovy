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

package org.boozallen.plugins.jte.binding

import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.utils.RunUtils
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.ExtensionPoint
import hudson.ExtensionList 
import jenkins.model.Jenkins 

abstract class TemplatePrimitiveInjector implements ExtensionPoint{
    // Optional. delegate injecting template primitives into the binding to the specific
    // implementations of TemplatePrimitive 
    static void doInject(TemplateConfigObject config, CpsScript script){}

    // Optional. do post processing of the config and binding. 
    static void doPostInject(TemplateConfigObject config, CpsScript script){}

    // used to get all injectors
    static ExtensionList<TemplatePrimitiveInjector> all(){
        return Impl.all()
    }

    static class Impl {// could not mock the abstract class

        // used to get all injectors
        static ExtensionList<TemplatePrimitiveInjector> all(){
            return Jenkins.getActiveInstance().getExtensionList(TemplatePrimitiveInjector)
        }
    }
}