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
package org.boozallen.plugins.jte.init.governance

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.model.ItemGroup
import jenkins.model.Jenkins
import net.sf.json.JSONObject
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.config.NullPipelineConfigurationProvider
import org.boozallen.plugins.jte.init.governance.config.PipelineConfigurationProvider
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerRequest
import hudson.model.Descriptor.FormException

/**
 * stores hierarchical configurations for JTE
 * <p>
 * The GovernanceTier optionally holds:
 * <ul>
 *     <li> pipeline configuration provided by a {@link PipelineConfigurationProvider} which provides:
 *     <ul>
 *         <li> a pipeline configuration
 *         <li> a default pipeline template
 *         <li> a pipeline catalog of named pipeline templates
 *     </ul>
 *     <li> a list of {@link LibrarySource(s)} to provide libraries
 * </ul>
 */
class GovernanceTier extends AbstractDescribableImpl<GovernanceTier> implements Serializable{

    private static final long serialVersionUID = 1L
    PipelineConfigurationProvider configurationProvider = new NullPipelineConfigurationProvider()
    List<LibrarySource> librarySources

    /*
       returns the job's GovernanceTier hierarchy in ascending order
       for governance, call .reverse() on the returned array to go top down
       for scoping of props, you can just iterate on the list
   */

    static List<GovernanceTier> getHierarchy(WorkflowJob job){
        List<GovernanceTier> h = []

        // folder pipeline configs
        ItemGroup<?> parent = job.getParent()
        while(parent instanceof AbstractFolder){
            GovernanceTier tier = parent.getProperties().get(TemplateConfigFolderProperty)?.getTier()
            if (tier){
                h.push(tier)
            }
            parent = parent.getParent()
        }

        // global config
        GovernanceTier tier = TemplateGlobalConfig.get().getTier()
        if (tier){
            h.push(tier)
        }
        return h
    }

    // jenkins requires this be here
    @SuppressWarnings('UnnecessaryConstructor')
    @DataBoundConstructor
    GovernanceTier(){}

    @SuppressWarnings('ParameterReassignment')
    @DataBoundSetter
    void setConfigurationProvider(PipelineConfigurationProvider configurationProvider){
        if(configurationProvider == null){
            configurationProvider = new NullPipelineConfigurationProvider()
        }
        this.configurationProvider = configurationProvider
    }

    PipelineConfigurationProvider getConfigurationProvider(){
        return configurationProvider
    }

    @DataBoundSetter
    void setLibrarySources(List<LibrarySource> librarySources){
        this.librarySources = librarySources
    }

    List<LibrarySource> getLibrarySources(){
        return librarySources
    }

    PipelineConfigurationObject getConfig(FlowExecutionOwner owner) throws Exception{
        return configurationProvider.getConfig(owner)
    }

    String getJenkinsfile(FlowExecutionOwner owner) throws Exception {
        return configurationProvider.getJenkinsfile(owner)
    }

    String getTemplate(FlowExecutionOwner owner, String template) throws Exception {
        return configurationProvider.getTemplate(owner, template)
    }

    protected Object readResolve(){
        if(configurationProvider == null){
            configurationProvider = new NullPipelineConfigurationProvider()
        }
        return this
    }

    @Extension
    final static class DescriptorImpl extends Descriptor<GovernanceTier> {

        static List<LibraryProvider.LibraryProviderDescriptor> getLibraryProviders(){
            return Jenkins.get().getExtensionList(LibraryProvider.LibraryProviderDescriptor)
        }

        static List<PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor> getPipelineConfigurationProviders(){
            return Jenkins.get().getExtensionList(PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor)
        }

        @Override
        GovernanceTier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            GovernanceTier tier = (GovernanceTier) super.newInstance(req, formData)
            return tier.librarySources?.isEmpty() ? null : tier
        }

        Descriptor getDefaultConfigurationProvider(){
            return Jenkins.get().getDescriptor(NullPipelineConfigurationProvider)
        }
    }

}
