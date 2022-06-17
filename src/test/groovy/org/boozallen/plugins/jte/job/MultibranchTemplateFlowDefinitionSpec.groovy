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

import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.extensions.GitSCMExtension
import jenkins.branch.BranchProperty
import jenkins.branch.BranchSource
import jenkins.branch.DefaultBranchPropertyStrategy
import jenkins.plugins.git.GitSCMSource
import jenkins.plugins.git.GitSampleRepoRule
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.init.governance.config.ScmPipelineConfigurationProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.governance.libs.ScmLibraryProvider
import org.eclipse.jgit.lib.SubmoduleConfig
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

class MultibranchTemplateFlowDefinitionSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    @Rule GitSampleRepoRule appRepo = new GitSampleRepoRule();
    @Rule GitSampleRepoRule libRepo = new GitSampleRepoRule();

    // add the new library source for each test
    def setup() {
        appRepo.init()
        libRepo.init()
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/286")
    def "multibranch pipeline not finding global libraries"(){
        given:
        // create SCM library source
        libRepo.write("gradle/steps/build.groovy", "void call(){ println 'gradle build' }")
        libRepo.git("add", "*")
        libRepo.git("commit", "--all", "--message=init")
        GitSCM libSCM = new GitSCM(
            GitSCM.createRepoList(libRepo.toString(), null),
            Collections.singletonList(new BranchSpec("*/master")),
            false,
            Collections.<SubmoduleConfig>emptyList(),
            null,
            null,
            Collections.<GitSCMExtension>emptyList()
        )
        ScmLibraryProvider provider = new ScmLibraryProvider()
        provider.setScm(libSCM)
        LibrarySource libSource = new LibrarySource(provider)
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = new GovernanceTier()
        List<LibrarySource> sources = [ libSource ]
        tier.setLibrarySources(sources)
        global.setTier(tier)

        // create multibranch project
        WorkflowMultiBranchProject project = jenkins.createProject(WorkflowMultiBranchProject)
        project.setProjectFactory(new TemplateBranchProjectFactory())
        // populate app repo
        appRepo.write("Jenkinsfile", "build()")
        appRepo.write(ScmPipelineConfigurationProvider.CONFIG_FILE, "libraries{ gradle }")
        appRepo.git("add", "*")
        appRepo.git("commit", "--all", "--message=init")
        // add app repo to project
        project.getSourcesList().add(new BranchSource(new GitSCMSource(null, appRepo.toString(), "", "*", "", false), new DefaultBranchPropertyStrategy(new BranchProperty[0])))
        when:
        // trigger indexing to create and run job for master
        project.scheduleBuild2(0).getFuture().get()
        WorkflowJob job = project.getItem("master")
        jenkins.waitUntilNoActivity()
        WorkflowRun run = job.getLastBuild()
        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("gradle build", run)
    }

}
