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
package org.boozallen.plugins.jte.init.governance.config

import hudson.Extension
import hudson.Util
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.kohsuke.stapler.DataBoundConstructor

/**
 * A named pipeline template defined in the Jenkins console.
 * <p>
 * The {@link ConsolePipelineConfigurationProvider} maintains a list of {@code ConsolePipelineTemplates}
 * that act as named templates in the pipeline catalog.
 */
class ConsoleDefaultPipelineTemplate extends AbstractDescribableImpl<ConsoleDefaultPipelineTemplate> implements Serializable{

    private static final long serialVersionUID = 1L
    boolean provideDefaultTemplate
    String defaultTemplate

    @DataBoundConstructor
    ConsoleDefaultPipelineTemplate(boolean provideDefaultTemplate, String defaultTemplate){
        this.provideDefaultTemplate = provideDefaultTemplate
        this.defaultTemplate = provideDefaultTemplate ? Util.fixEmptyAndTrim(defaultTemplate) : null
    }

    boolean getProvideDefaultTemplate(){
        return provideDefaultTemplate
    }

    String getDefaultTemplate(){
        return defaultTemplate
    }

    @Extension
    final static class DescriptorImpl extends Descriptor<ConsoleDefaultPipelineTemplate> {}

}
