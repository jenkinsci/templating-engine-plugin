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
package org.boozallen.plugins.jte.init.primitives

import hudson.ExtensionList
import hudson.ExtensionPoint
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Classes wishing to parse the aggregated pipeline configuration and create
 * a TemplatePrimitive to inject into the TemplateBinding should extend this
 * class to become discoverable during initialization by the PipelineDecorator
 */
@SuppressWarnings(['EmptyMethodInAbstractClass', 'UnusedMethodParameter'])
abstract class TemplatePrimitiveInjector implements ExtensionPoint{

    /**
     * find all registered injectors
     * @return all registered injectors
     */
    static ExtensionList<TemplatePrimitiveInjector> all(){
        return Jenkins.get().getExtensionList(TemplatePrimitiveInjector)
    }

    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){}
    void doPostInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){}

}
