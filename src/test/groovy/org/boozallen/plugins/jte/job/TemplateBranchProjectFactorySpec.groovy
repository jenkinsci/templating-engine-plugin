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

import jenkins.branch.BranchSource
import jenkins.branch.OrganizationFolder
import jenkins.plugins.git.GitSCMSource
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.scm.impl.SingleSCMNavigator
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class TemplateBranchProjectFactorySpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    @Rule GitSampleRepoRule repo = new GitSampleRepoRule()

    String templatePath = 'dir/Jenkinsfile'
    String configPath = 'dir/pipeline_config.groovy'

    def setup() {
        String pipelineConfigContents = '''
        block{
            custom = 'hello'
        }
        '''
        String pipelineTemplateContents = '''
        node {
            stage('Build') {
                echo "build number: ${env.BUILD_NUMBER}"
                echo "branch: ${env.BRANCH_NAME}"
                echo "custom: ${pipelineConfig.block.custom}"
            }
        }
        '''
        repo.init()
        repo.write(templatePath, pipelineTemplateContents)
        repo.write(configPath, pipelineConfigContents)
        repo.git('add', '*')
        repo.git('commit', '--message=init')
    }

    def "Specify custom template and configuration path with TemplateBranchProjectFactory"() {
        given:
        WorkflowMultiBranchProject mp = jenkins.createProject(WorkflowMultiBranchProject)
        mp.getSourcesList().add(new BranchSource(new GitSCMSource(null, repo.toString(), "", "*", "", false)))
        TemplateBranchProjectFactory factory = new TemplateBranchProjectFactory()
        factory.setConfigurationPath(configPath)
        factory.setScriptPath(templatePath)
        factory.setFilterBranches(true)
        mp.setProjectFactory(factory)

        when:
        mp.scheduleBuild2(0).getFuture().get()
        jenkins.waitUntilNoActivity()
        WorkflowRun run = mp.getItem('master').getLastBuild()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('custom: hello', run)
        jenkins.assertLogContains('branch: master', run)
        jenkins.assertLogContains('build number: 1', run)
    }

    def "Specify custom template and configuration path with TemplateMultiBranchProjectFactory"() {
        given:
        OrganizationFolder folder = jenkins.createProject(OrganizationFolder)
        folder.getNavigators().add(new SingleSCMNavigator('repo', [new GitSCMSource(null, repo.toString(), "", "*", "", false)]))
        TemplateMultiBranchProjectFactory factory = new TemplateMultiBranchProjectFactory()
        factory.setConfigurationPath(configPath)
        factory.setScriptPath(templatePath)
        factory.setFilterBranches(true)
        folder.getProjectFactories().add(factory)

        when:
        folder.scheduleBuild2(0).getFuture().get()
        jenkins.waitUntilNoActivity()
        WorkflowRun run = folder.getItem('repo').getItem('master').getLastBuild()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('custom: hello', run)
        jenkins.assertLogContains('branch: master', run)
        jenkins.assertLogContains('build number: 1', run)
    }

}
