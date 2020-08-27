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

import com.cloudbees.hudson.plugins.folder.Folder
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.config.NullPipelineConfigurationProvider
import org.boozallen.plugins.jte.init.governance.config.PipelineConfigurationProvider
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Specification

class GovernanceTierSpec extends Specification{

    @Rule JenkinsRule jenkins = new JenkinsRule()

    @WithoutJenkins
    def "new GovernanceTier's PipelienConfigurationProvider defaults to NullPipelineConfigurationProvider"(){
        when:
        GovernanceTier tier = new GovernanceTier()

        then:
        tier.getConfigurationProvider() instanceof NullPipelineConfigurationProvider
    }

    @WithoutJenkins
    def "getConfig() returns configurationProvider's PipelineConfigurationObject"(){
        when:
        PipelineConfigurationObject configObject = Mock()
        PipelineConfigurationProvider configProvider = Mock()
        configProvider.getConfig(_) >> configObject
        GovernanceTier tier = new GovernanceTier()
        tier.setConfigurationProvider(configProvider)

        then:
        tier.getConfig(Mock(FlowExecutionOwner)) == configObject
    }

    def "hierarchy: top-level job without global config"(){
        setup:
        WorkflowJob job = jenkins.createProject(WorkflowJob)

        when:
        List<GovernanceTier> hierarchy = GovernanceTier.getHierarchy(job)

        then:
        hierarchy.isEmpty()
    }

    def "hierarchy: top-level job with global config"(){
        setup:
        WorkflowJob job = jenkins.createProject(WorkflowJob)
        GovernanceTier tier = Mock()
        TemplateGlobalConfig.get().setTier(tier)

        when:
        List<GovernanceTier> hierarchy = GovernanceTier.getHierarchy(job)

        then:
        hierarchy == [ tier ]
    }

    def "hierarchy: folder job without global config"(){
        setup:
        Folder folder = jenkins.createProject(Folder, jenkins.createUniqueProjectName())
        GovernanceTier folderTier = new GovernanceTier()
        TemplateConfigFolderProperty prop = new TemplateConfigFolderProperty(folderTier)
        folder.getProperties().add(prop)

        WorkflowJob job = folder.createProject(WorkflowJob, jenkins.createUniqueProjectName())

        when:
        List<GovernanceTier> hierarchy = GovernanceTier.getHierarchy(job)

        then:
        hierarchy == [ folderTier ]
    }

    def "hierarchy: folder job with global config"(){
        setup:
        GovernanceTier globalTier = new GovernanceTier()
        TemplateGlobalConfig.get().setTier(globalTier)

        Folder folder = jenkins.createProject(Folder, jenkins.createUniqueProjectName())
        GovernanceTier folderTier = new GovernanceTier()
        TemplateConfigFolderProperty prop = new TemplateConfigFolderProperty(folderTier)
        folder.getProperties().add(prop)

        WorkflowJob job = folder.createProject(WorkflowJob, jenkins.createUniqueProjectName())

        when:
        List<GovernanceTier> hierarchy = GovernanceTier.getHierarchy(job)

        then:
        hierarchy == [ folderTier, globalTier ]
    }

}
