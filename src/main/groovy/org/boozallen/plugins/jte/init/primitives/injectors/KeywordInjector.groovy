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
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.JteNamespace
import org.boozallen.plugins.jte.init.primitives.JteNamespace.Namespace
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates Keywords and populates the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class KeywordInjector extends TemplatePrimitiveInjector {

    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("Keyword.groovy").text
        return parseClass(classText)
    }

    static void populateNamespace(JteNamespace jte, TemplatePrimitive primitive){
        Namespace n = jte.getNamespace(key)
        if(!n) {
            // namespace doesn't exist yet.. create, push primitive, add
            n = new KeywordNamespace()
            n.push(primitive)
            jte.addNamespace(n)
        } else if(!(n instanceof KeywordNamespace)){
            // namespace exists but isn't from this injector somehow?
            throw new Exception("JTE Namespace conflict for name: ${key}")
        } else {
            // namespace exists.. just add primitive
            n.push(primitive)
        }
    }

    static class KeywordNamespace extends Namespace {
        String name = getKey()
        LinkedHashMap primitives = [:]
        @Override void push(TemplatePrimitive primitive){
            String name = primitive.getName()
            primitives[name] = primitive.getValue()
        }
        Object getProperty(String name){
            if(!primitives.containsKey(name)){
                throw new Exception("Keyword ${name} not found")
            }
            return primitives[name]
        }
    }

    static String getKey(){ return "keywords" }

    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        Class keywordClass = getPrimitiveClass()
        LinkedHashMap aggregatedConfig = config.getConfig()
        def keywords = aggregatedConfig[key]
        keywords.each{ name, value ->
            binding.setVariable(name, keywordClass.newInstance(
                name: name,
                value: value,
                injector: this.getClass()
            ))
        }
    }

}
