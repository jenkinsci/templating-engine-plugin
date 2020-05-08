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
import hudson.model.Item
import hudson.model.ItemGroup
import hudson.model.TaskListener
import hudson.model.Action
import jenkins.branch.MultiBranchProjectFactory
import jenkins.branch.MultiBranchProjectFactoryDescriptor
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowBranchProjectFactory
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import jenkins.branch.MultiBranchProject
import jenkins.branch.OrganizationFolder
import jenkins.model.TransientActionFactory
import jenkins.scm.api.SCMSource
import jenkins.scm.api.SCMSourceCriteria
import org.jenkinsci.plugins.workflow.cps.Snippetizer

public class TemplateMultiBranchProjectFactory extends MultiBranchProjectFactory.BySCMSourceCriteria {

    Boolean filterBranches

    @DataBoundConstructor public TemplateMultiBranchProjectFactory() { }

    public Object readResolve() {
        if (this.filterBranches == null) {
            this.filterBranches = false;
        }
        return this;
    }

    @DataBoundSetter
    public void setFilterBranches(Boolean filterBranches){
        this.filterBranches = filterBranches
    }

    public Boolean getFilterBranches(){
        return filterBranches
    }


    private AbstractWorkflowBranchProjectFactory newProjectFactory() {
        TemplateBranchProjectFactory factory = new TemplateBranchProjectFactory()
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

    @Override public final void updateExistingProject(MultiBranchProject<?, ?> project, Map<String, Object> attributes, TaskListener listener) throws IOException, InterruptedException {
        if (project instanceof WorkflowMultiBranchProject) {
            customize((WorkflowMultiBranchProject) project)
        } // otherwise got recognized by something else before, oh well
    }

    @Override protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return newProjectFactory().getSCMSourceCriteria(source)
    }

    @Extension public static class PerFolderAdder extends TransientActionFactory<OrganizationFolder> {

        @Override public Class<OrganizationFolder> type() {
            return OrganizationFolder.class
        }

        @Override public Collection<? extends Action> createFor(OrganizationFolder target) {
            if (target.getProjectFactories().get(TemplateMultiBranchProjectFactory.class) != null && target.hasPermission(Item.EXTENDED_READ)) {
                return Collections.singleton(new Snippetizer.LocalAction())
            } else {
                return Collections.emptySet()
            }
        }

    }

    @Extension public static class DescriptorImpl extends MultiBranchProjectFactoryDescriptor {

        /* 
            returning the factory will result in this option being
            selected by default in multibranch factory jobs (github org job)
        */
        @Override public MultiBranchProjectFactory newInstance() {
            return null 
        }

        @Override public String getDisplayName() {
            return "Jenkins Templating Engine"
        }

    }

}
