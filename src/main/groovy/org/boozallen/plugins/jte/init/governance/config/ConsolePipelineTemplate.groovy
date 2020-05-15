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

import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.Util
import jenkins.model.Jenkins
import hudson.RelativePath
import hudson.util.FormValidation
import org.kohsuke.stapler.QueryParameter

public class ConsolePipelineTemplate extends AbstractDescribableImpl<ConsolePipelineTemplate> implements Serializable{

    String name
    String template

    @DataBoundConstructor public ConsolePipelineTemplate(){}

    @DataBoundSetter
    public void setName(String name){
        this.name = Util.fixEmptyAndTrim(name)
    }

    public String getName(){
        return name
    }

    @DataBoundSetter
    public void setTemplate(String template){
        this.template = Util.fixEmptyAndTrim(template)
    }

    public String getTemplate(){
        return template
    }

    @Extension public final static class DescriptorImpl extends Descriptor<ConsolePipelineTemplate> {}
}
