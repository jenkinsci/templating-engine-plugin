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
import org.boozallen.plugins.jte.init.primitives.NamespaceCollector
import org.boozallen.plugins.jte.init.primitives.NamespaceCollector.PrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.JTEException
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun

/**
 * creates ApplicationEnvironments and populates the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class ApplicationEnvironmentInjector extends TemplatePrimitiveInjector {

    static private final String KEY = "application_environments"

    static Class getPrimitiveClass() {
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("ApplicationEnvironment.groovy").text
        return parseClass(classText)
    }

    @SuppressWarnings('NoDef')
    @Override
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding) {
        // if a run can be found, create a PrimitiveNamespace for the application environments
        WorkflowRun run = flowOwner.run()
        if(!run){
            throw new JTEException("Invalid Context. Cannot determine run.")
        }

        PrimitiveNamespace appEnvs = NamespaceCollector.createNamespace(KEY)

        // populate the namespace with application environments from pipeline config
        LinkedHashMap aggregatedConfig = config.getConfig()
        Class appEnvClass = getPrimitiveClass()
        ArrayList createdEnvs = []
        aggregatedConfig[KEY].each { name, appEnvConfig ->
            def env = appEnvClass.newInstance(name, appEnvConfig)
            createdEnvs << env
        }
        createdEnvs.eachWithIndex { env, index ->
            def previous = index ? createdEnvs[index - 1] : null
            def next = (index == (createdEnvs.size() - 1)) ? null : createdEnvs[index + 1]
            env.setPrevious(previous)
            env.setNext(next)
            appEnvs.add(env)
        }

        // add the namespace to the collector and save it on the run
        NamespaceCollector namespaceCollector = run.getAction(NamespaceCollector)
        if(namespaceCollector == null){
            namespaceCollector = new NamespaceCollector()
        }
        namespaceCollector.addNamespace(appEnvs)
        run.addOrReplaceAction(namespaceCollector)
    }
}