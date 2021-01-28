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
import org.boozallen.plugins.jte.init.primitives.PrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.RunAfter
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates Stages and populates the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class StageInjector extends TemplatePrimitiveInjector {

    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("Stage.groovy").text
        return parseClass(classText)
    }

    private static final String KEY = "stages"
    private static final String TYPE_DISPLAY_NAME = "Stage"
    private static final String NAMESPACE_KEY = KEY

    static PrimitiveNamespace createNamespace(){
        return new CallableNamespace(name: getNamespaceKey(), typeDisplayName: TYPE_DISPLAY_NAME)
    }

    static String getNamespaceKey(){
        return NAMESPACE_KEY
    }

    @Override
    @RunAfter([LibraryStepInjector, DefaultStepInjector, TemplateMethodInjector])
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        Class stageClass = getPrimitiveClass()
        LinkedHashMap aggregatedConfig = config.getConfig()
        aggregatedConfig[KEY].each{ name, steps ->
            List<String> stepNames = steps.keySet() as List<String>
            binding.setVariable(name, stageClass.newInstance(
                name: name,
                steps: stepNames,
                injector: this.getClass()
            ))
        }
    }

    @Override
    void validateBinding(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        LinkedHashMap aggregatedConfig = config.getConfig()
        LinkedHashMap stagesWithUndefinedSteps = [:]
        aggregatedConfig[KEY].each{ name, stageConfig ->
            List<String> steps = stageConfig.keySet() as List<String>
            List<String> undefinedSteps = []
            steps.each{ step ->
                if(!binding.hasStep(step)){
                    undefinedSteps << step
                }
            }
            if(undefinedSteps){
                stagesWithUndefinedSteps[name] = undefinedSteps
            }
        }
        if(stagesWithUndefinedSteps){
            List<String> error = [
                    "There are Stages defined that reference steps that do not exist.",
                    "Consider adding step names to the template_methods block"
            ]
            stagesWithUndefinedSteps.each{ name, steps ->
                error << "- ${name}: ${steps.join(", ")}".toString()
            }
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printError(error.join("\n"))
            throw new JTEException("There are Stages defined that require undefined steps")
        }
    }

    static class StageContext implements Serializable {
        private static final long serialVersionUID = 1L
        String name
        Map args = [:]
    }

}
