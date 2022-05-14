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
package org.boozallen.plugins.jte.job

import hudson.Extension
import hudson.Util
import hudson.model.Action
import hudson.model.ItemGroup
import hudson.model.Item
import hudson.model.TaskListener
import jenkins.branch.MultiBranchProject
import jenkins.branch.MultiBranchProjectFactory
import jenkins.branch.MultiBranchProjectFactoryDescriptor
import jenkins.branch.OrganizationFolder
import jenkins.model.TransientActionFactory
import jenkins.scm.api.SCMSource
import jenkins.scm.api.SCMSourceCriteria
import org.boozallen.plugins.jte.init.governance.config.ScmPipelineConfigurationProvider
import org.jenkinsci.plugins.workflow.cps.Snippetizer
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowBranchProjectFactory
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

/**
 * registers {@link MultibranchTemplateFlowDefinition} as an option for Organization jobs
 * <p>
 * Organization jobs typically represent a group of source code repositories
 */
class TemplateMultiBranchProjectFactory extends MultiBranchProjectFactory.BySCMSourceCriteria {

    String scriptPath
    String configurationPath
    Boolean filterBranches

    // jenkins requires this be here
    @SuppressWarnings('UnnecessaryConstructor')
    @DataBoundConstructor
    TemplateMultiBranchProjectFactory(){}

    Object readResolve() {
        if (this.scriptPath == null) {
            this.scriptPath = 'Jenkinsfile'
        }
        if (this.configurationPath == null) {
            this.configurationPath = ScmPipelineConfigurationProvider.CONFIG_FILE
        }
        if (this.filterBranches == null) {
            this.filterBranches = false
        }
        return this
    }

    @DataBoundSetter
    void setScriptPath(String scriptPath){
        this.scriptPath = Util.fixEmptyAndTrim(scriptPath) ?: 'Jenkinsfile'
    }

    String getScriptPath(){
        return scriptPath
    }

    @DataBoundSetter
    void setConfigurationPath(String configurationPath){
        this.configurationPath = Util.fixEmptyAndTrim(configurationPath) ?: ScmPipelineConfigurationProvider.CONFIG_FILE
    }

    String getConfigurationPath(){
        return configurationPath
    }

    @DataBoundSetter
    void setFilterBranches(Boolean filterBranches){
        this.filterBranches = filterBranches
    }

    Boolean getFilterBranches(){
        return filterBranches
    }

    @Override
    final void updateExistingProject(MultiBranchProject<?, ?> project, Map<String, Object> attributes, TaskListener listener) throws IOException, InterruptedException {
        if (project instanceof WorkflowMultiBranchProject) {
            customize((WorkflowMultiBranchProject) project)
        } // otherwise got recognized by something else before, oh well
    }

    private AbstractWorkflowBranchProjectFactory newProjectFactory() {
        TemplateBranchProjectFactory factory = new TemplateBranchProjectFactory()
        factory.setScriptPath(this.scriptPath)
        factory.setConfigurationPath(this.configurationPath)
        factory.setFilterBranches(this.filterBranches)
        return factory
    }

    protected void customize(WorkflowMultiBranchProject project){
        project.setProjectFactory(newProjectFactory())
    }

    @Override protected final WorkflowMultiBranchProject doCreateProject(ItemGroup<?> parent, String name, Map<String,Object> attributes){
        WorkflowMultiBranchProject project = new WorkflowMultiBranchProject(parent, name)
        customize(project)
        return project
    }

    @Override protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return newProjectFactory().getSCMSourceCriteria(source)
    }

    @Extension
    static class PerFolderAdder extends TransientActionFactory<OrganizationFolder> {

        @Override
        Class<OrganizationFolder> type() {
            return OrganizationFolder
        }

        @Override
        Collection<? extends Action> createFor(OrganizationFolder target) {
            if (target.getProjectFactories().get(TemplateMultiBranchProjectFactory) != null && target.hasPermission(Item.EXTENDED_READ)) {
                return Collections.singleton(new Snippetizer.LocalAction())
            }
            return Collections.emptySet()
        }

    }

    @Extension
    static class DescriptorImpl extends MultiBranchProjectFactoryDescriptor {

        /*
            returning the factory will result in this option being
            selected by default in multibranch factory jobs (github org job)
        */
        @Override
        MultiBranchProjectFactory newInstance() {
            return null
        }

        @Override
        String getDisplayName() {
            return "Jenkins Templating Engine"
        }

    }

}
