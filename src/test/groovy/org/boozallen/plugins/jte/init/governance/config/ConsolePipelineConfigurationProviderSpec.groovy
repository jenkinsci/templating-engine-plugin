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

import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import spock.lang.Specification

class ConsolePipelineConfigurationProviderSpec extends Specification {

    def "when pipeline configuration is provided getConfig returns correct config object"() {
        given:
        ConsolePipelineConfiguration pipelineConfig = new ConsolePipelineConfiguration(true, 'a =1 ')
        ConsoleDefaultPipelineTemplate template = new ConsoleDefaultPipelineTemplate(false, null)

        List<ConsoleNamedPipelineTemplate> pipelineCatalog = []
        def c = new ConsolePipelineConfigurationProvider(template, pipelineConfig, pipelineCatalog)

        // mocks necessary to parse config
        FlowExecutionOwner mockOwner = GroovyMock {
            run() >> GroovyMock(WorkflowRun)
            asBoolean() >> true
        }

        EnvActionImpl env = Mock()
        env.getProperty('someField') >> 'envProperty'

        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> env

        when:
        PipelineConfigurationObject conf = c.getConfig(mockOwner)

        then:
        conf.getConfig() == [ a: 1 ]
    }

    def "when pipeline configuration is not provided getConfig returns null"() {
        given:
        List<ConsoleNamedPipelineTemplate> pipelineCatalog = []
        ConsolePipelineConfiguration pipelineConfig = new ConsolePipelineConfiguration(false, null)
        ConsoleDefaultPipelineTemplate template = new ConsoleDefaultPipelineTemplate(false, null)
        def c = new ConsolePipelineConfigurationProvider(template, pipelineConfig, pipelineCatalog)

        // mocks necessary to parse config
        FlowExecutionOwner mockOwner = GroovyMock {
            run() >> GroovyMock(WorkflowRun)
            asBoolean() >> true
        }

        EnvActionImpl env = Mock()
        env.getProperty('someField') >> 'envProperty'

        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> env

        when:
        PipelineConfigurationObject conf = c.getConfig(mockOwner)

        then:
        conf == null
    }

    def "When Jenkinsfile is provided, getJenkinsfile returns Jenkinsfile"() {
        given:
        List<ConsoleNamedPipelineTemplate> pipelineCatalog = []
        ConsolePipelineConfiguration pipelineConfig = new ConsolePipelineConfiguration(false, null)
        ConsoleDefaultPipelineTemplate template = new ConsoleDefaultPipelineTemplate(true, 'default jenkinsfile')

        def c = new ConsolePipelineConfigurationProvider(template, pipelineConfig, pipelineCatalog)

        when:
        String jenkinsfile = c.getJenkinsfile()

        then:
        jenkinsfile == 'default jenkinsfile'
    }

    def "fetch nonexistent named template returns null"() {
        given:
        List<ConsoleNamedPipelineTemplate> pipelineCatalog = []
        ConsolePipelineConfiguration pipelineConfig = new ConsolePipelineConfiguration(false, null)
        ConsoleDefaultPipelineTemplate template = new ConsoleDefaultPipelineTemplate(false, null)
        def c = new ConsolePipelineConfigurationProvider(template, pipelineConfig, pipelineCatalog)

        FlowExecutionOwner mockOwner = GroovyMock {
            run() >> GroovyMock(WorkflowRun)
            asBoolean() >> true
        }

        when:
        String namedTemplate = c.getTemplate(mockOwner, 'nonexistent')

        then:
        namedTemplate == null
    }

    def "fetch named template returns correct template"() {
        given:
        List<ConsoleNamedPipelineTemplate> pipelineCatalog = [
            new ConsoleNamedPipelineTemplate(
                name: 'myCoolTemplate',
                template: 'named template!'
            )
        ]
        def c = new ConsolePipelineConfigurationProvider(new ConsoleDefaultPipelineTemplate(false, null),
                new ConsolePipelineConfiguration(false, null),
                pipelineCatalog)

        FlowExecutionOwner mockOwner = GroovyMock {
            run() >> GroovyMock(WorkflowRun)
            asBoolean() >> true
        }

        when:
        String namedTemplate = c.getTemplate(mockOwner, 'myCoolTemplate')

        then:
        namedTemplate == 'named template!'
    }

}
