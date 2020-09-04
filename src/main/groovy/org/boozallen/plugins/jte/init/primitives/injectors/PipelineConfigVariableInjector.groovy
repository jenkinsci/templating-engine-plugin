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
import org.boozallen.plugins.jte.init.primitives.JteNamespace
import org.boozallen.plugins.jte.init.primitives.JteNamespace.Namespace
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * injects the aggregated pipeline configuration as a variable called pipelineConfig into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class PipelineConfigVariableInjector extends TemplatePrimitiveInjector {

    static void populateNamespace(JteNamespace jte, TemplatePrimitive primitive){
        Namespace n = jte.getNamespace(key)
        if(!n) {
            // namespace doesn't exist yet.. create, push primitive, add
            n = new PipelineConfigNamespace()
            n.push(primitive)
            jte.addNamespace(n)
        } else if(!(n instanceof PipelineConfigNamespace)){
            // namespace exists but isn't from this injector somehow?
            throw new Exception("JTE Namespace conflict for name: ${key}")
        } else {
            // namespace exists.. just add primitive
            n.push(primitive)
        }
    }

    static class PipelineConfigNamespace extends Namespace {
        String name = getKey()
        LinkedHashMap pipelineConfig
        @Override void push(TemplatePrimitive primitive){
            pipelineConfig = primitive.getValue()
        }
        Object getProperty(String name){
            return pipelineConfig[name]
        }
    }

    static String getKey(){ return "pipelineConfig" }

    @SuppressWarnings('NoDef')
    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        Class keywordClass = KeywordInjector.getPrimitiveClass()
        def pipelineConfig = keywordClass.newInstance(
            name: getKey(),
            injector: this.getClass(),
            value: config.getConfig(),
            preLockException: "Variable ${getKey()} reserved for accessing the aggregated pipeline configuration",
            postLockException: "Variable ${getKey()} reserved for accessing the aggregated pipeline configuration"
        )
        binding.setVariable(getKey(), pipelineConfig)
    }

}
