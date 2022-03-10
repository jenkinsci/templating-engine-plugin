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

import hudson.model.TaskListener
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.scm.NullSCM
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.FileSystemWrapperFactory
import org.boozallen.plugins.jte.util.TestFlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class AdHocTemplateFlowDefinitionSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    @Rule GitSampleRepoRule repo = new GitSampleRepoRule()

    TestFlowExecutionOwner flowOwner
    PrintStream logger = Mock()

    def setup() {
        WorkflowJob job = GroovyMock()
        job.asBoolean() >> true
        WorkflowRun run = GroovyMock()
        run.getParent() >> job
        TaskListener listener = Mock()
        listener.getLogger() >> logger
        flowOwner = Mock()
        flowOwner.getListener() >> listener
        flowOwner.run() >> run
    }

    GitSCM createSCM(GitSampleRepoRule _repo) {
        return new GitSCM(
                GitSCM.createRepoList(_repo.toString(), null),
                Collections.singletonList(new BranchSpec('*/master')),
                false,
                Collections.<SubmoduleConfig>emptyList(),
                null,
                null,
                Collections.<GitSCMExtension>emptyList()
        )
    }

    def "Scm Adhoc hasConfig, hasTemplate return false if empty path"() {
        ScmAdHocTemplateFlowDefinitionConfiguration flowDefConfig =
                new ScmAdHocTemplateFlowDefinitionConfiguration(new NullSCM(), '', '')

        expect:
        !flowDefConfig.hasConfig(flowOwner)
        !flowDefConfig.hasTemplate(flowOwner)
    }

    def "Scm AdHoc ... getTemplate: returns contents of scm template file, empty config path -> null config"() {
        given:
        String baseDir = 'pipeline-configuration'
        String pipelineConfigPath = ''
        String pipelineTemplatePath = "${baseDir}/pipeline_templates/someTemplate"
        String pipelineTemplateContents = 'the template'
        repo.init()
        repo.write(pipelineTemplatePath, pipelineTemplateContents)
        repo.git('add', '*')
        repo.git('commit', '--message=init')
        GitSCM scm = createSCM(repo)
        ScmAdHocTemplateFlowDefinitionConfiguration flowDefConfig =
                new ScmAdHocTemplateFlowDefinitionConfiguration(scm, pipelineConfigPath, pipelineTemplatePath)

        FileSystemWrapper fsw = new FileSystemWrapper(owner: flowOwner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapperFactory, global: true)
        FileSystemWrapperFactory.create(flowOwner, scm) >> fsw

        def definition = new AdHocTemplateFlowDefinition(flowDefConfig)

        expect:
        flowDefConfig.hasTemplate(flowOwner)
        flowDefConfig.getTemplate(flowOwner) == pipelineTemplateContents
        definition.getTemplate(flowOwner) == pipelineTemplateContents
        !flowDefConfig.hasConfig(flowOwner)
        definition.getPipelineConfiguration(flowOwner) == null
    }

    def "Scm AdHoc ...getTemplate, getConfig: returns contents"() {
        given:
        PipelineConfigurationObject configurationObject
        PipelineConfigurationObject definitionConfig
        String baseDir = 'pipeline-configuration'
        String pipelineConfigPath = "${baseDir}/pipeline_templates/pipeline_config.0.groovy"
        String pipelineConfigContents = '''
keywords{
  main = "/(M|m)ain/"
}
'''
        String pipelineTemplatePath = "${baseDir}/pipeline_templates/someTemplate"
        String pipelineTemplateContents = 'the template'
        repo.init()
        repo.write(pipelineTemplatePath, pipelineTemplateContents)
        repo.write(pipelineConfigPath, pipelineConfigContents)
        repo.git('add', '*')
        repo.git('commit', '--message=init')
        GitSCM scm = createSCM(repo)
        ScmAdHocTemplateFlowDefinitionConfiguration flowDefConfig =
                new ScmAdHocTemplateFlowDefinitionConfiguration(scm, pipelineConfigPath, pipelineTemplatePath)

        FileSystemWrapper fsw = new FileSystemWrapper(owner: flowOwner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapperFactory, global: true)
        FileSystemWrapperFactory.create(flowOwner, scm) >> fsw

        def definition = new AdHocTemplateFlowDefinition(flowDefConfig)
        when:
        configurationObject = flowDefConfig.getConfig(flowOwner)
        definitionConfig = definition.getPipelineConfiguration(flowOwner)

        then:
        flowDefConfig.hasConfig(flowOwner)
        flowDefConfig.hasTemplate(flowOwner)
        flowDefConfig.getTemplate(flowOwner) == pipelineTemplateContents
        definition.getTemplate(flowOwner) == pipelineTemplateContents
        configurationObject != null
        configurationObject.config?.keywords?.main == '/(M|m)ain/'
        definitionConfig.config?.keywords?.main == '/(M|m)ain/'
    }

}
