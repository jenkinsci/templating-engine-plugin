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
import hudson.model.TaskListener
import jenkins.branch.MultiBranchProject
import jenkins.scm.api.SCMFile
import jenkins.scm.api.SCMProbeStat
import jenkins.scm.api.SCMSource
import jenkins.scm.api.SCMSourceCriteria
import org.boozallen.plugins.jte.init.governance.config.ScmPipelineConfigurationProvider
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowBranchProjectFactory
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

import javax.annotation.Nonnull

/**
 * Registers {@link MultibranchTemplateFlowDefinition} as an option for use with Multibranch Projects
 */
class TemplateBranchProjectFactory extends WorkflowBranchProjectFactory {

    String configurationPath
    Boolean filterBranches

    // jenkins requires this be here
    @SuppressWarnings('UnnecessaryConstructor')
    @DataBoundConstructor
    TemplateBranchProjectFactory(){}

    Object readResolve() {
        if (this.configurationPath == null) {
            this.configurationPath = ScmPipelineConfigurationProvider.CONFIG_FILE
        }
        if (this.filterBranches == null) {
            this.filterBranches = false
        }
        return this
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
    protected FlowDefinition createDefinition() {
        MultibranchTemplateFlowDefinition definition = new MultibranchTemplateFlowDefinition()
        definition.setScriptPath(this.scriptPath)
        definition.setConfigurationPath(this.configurationPath)
        return definition
    }

    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return new SCMSourceCriteria() {
            @Override
            boolean isHead(SCMSourceCriteria.Probe probe, TaskListener listener) throws IOException {
                // default behavior is to create jobs for each branch
                if(!filterBranches){
                    return true
                }

                // if user chose to filter branches, check for pipeline config file
                SCMProbeStat stat = probe.stat(configurationPath)
                switch (stat.getType()) {
                    case SCMFile.Type.NONEXISTENT:
                        if (stat.getAlternativePath() != null) {
                            listener.getLogger().format("      ‘%s’ not found (but found ‘%s’, search is case sensitive)%n", configurationPath, stat.getAlternativePath())
                        } else {
                            listener.getLogger().format("      ‘%s’ not found%n", configurationPath)
                        }
                        return false
                    case SCMFile.Type.DIRECTORY:
                        listener.getLogger().format("      ‘%s’ found but is a directory not a file%n", configurationPath)
                        return false
                    default:
                        listener.getLogger().format("      ‘%s’ found%n", configurationPath)
                        return true
                }
            }

            @Override
            int hashCode() {
                return getClass().hashCode()
            }

            @Override
            boolean equals(Object obj) {
                return getClass().isInstance(obj)
            }
        }
    }

    @Extension
    static class DescriptorDefaultImpl extends AbstractWorkflowBranchProjectFactory.AbstractWorkflowBranchProjectFactoryDescriptor {

        @Override
        boolean isApplicable(Class<? extends MultiBranchProject> clazz) {
            return MultiBranchProject.isAssignableFrom(clazz)
        }

        @Nonnull
        @Override
        String getDisplayName() {
            return "Jenkins Templating Engine"
        }

    }

}
