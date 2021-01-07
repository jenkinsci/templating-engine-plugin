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
package org.boozallen.plugins.jte.init.governance.config.dsl

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import spock.lang.Specification

class PipelineConfigurationDslSpec extends Specification {

    EnvVars env = GroovyMock(EnvVars)
    PipelineConfigurationDsl dsl = new PipelineConfigurationDsl(GroovyMock(FlowExecutionOwner){
        run() >> GroovyMock(WorkflowRun){
            getEnvironment(_) >> env
        }
        asBoolean() >> true
    })

    def "include Jenkins env var in configuration"(){
        setup:
        env.get("someField", _) >> "envProperty"
        String config = "a = env.someField"

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.config == [ a: "envProperty" ]
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Empty Config File'(){
        setup:
        String config = ""

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.config == [:]
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Flat Keys Configuration'(){
        setup:
        String config = """
        a = 3
        b = "hi"
        c = true
        """

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.config == [
            a: 3,
            b: "hi",
            c: true
        ]
    }

    def 'Nested Keys Configuration'(){
        setup:
        String config = """
        random = "hi"
        application_environments{
            dev{
                field = true
            }
            test{
                field = false
            }
        }
        blah{
            another{
                field = "hey"
            }
        }
        """

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.config == [
            random: "hi",
            application_environments: [
                dev: [
                    field: true
                ],
                test: [
                    field: false
                ]
            ],
            blah: [
                another: [
                    field: "hey"
                ]
            ]
        ]
    }

    def 'One Merge First Key'(){
        setup:
        String config = "@merge application_environments{}"

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.merge == [ "application_environments" ] as Set
    }

    def 'One Merge Nested Key'(){
        setup:
        String config = """
        application_environments{
            @merge dev{}
        }
        """

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.merge == [ "application_environments.dev" ] as Set
    }

    def 'Multi-Merge'(){
        setup:
        String config = """
        application_environments{
            @merge dev{}
            @merge test{}
        }
        """

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.merge == [ "application_environments.dev", "application_environments.test" ] as Set
    }

    def 'One Override First Key'(){
        when:
        String config = "@override application_environments{}"
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.override == [ "application_environments" ] as Set
    }

    def 'One Override Nested Key'(){
        when:
        String config = """
        application_environments{
            @override dev{}
        }
        """
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.override == [ "application_environments.dev" ] as Set
    }

    def 'Multi-Override'(){
        setup:
        String config = """
        application_environments{
            @override dev{}
            @override test{}
        }
        """

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.override == [ "application_environments.dev", "application_environments.test" ] as Set
    }

    def 'File Access Throws Security Exception'(){
        setup:
        String config = 'password = new File("/etc/passwd").text'

        when:
        dsl.parse(config)

        then:
        thrown(SecurityException)
    }

    def "nested blank entry results in empty hashmap"(){
        setup:
        String config = "application_environments{ dev }"

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.config == [
            application_environments: [
                dev: [:]
            ]
        ]
    }

    def "root blank entry results in empty hashmap"(){
        setup:
        String config = "field"

        when:
        PipelineConfigurationObject configObject = dsl.parse(config)

        then:
        configObject.getConfig() == [ field: [:] ]
    }

    def "syntax validation for unquoted values"(){
        setup:
        String config = "a = b"

        when:
        dsl.parse(config)

        then:
        TemplateConfigException ex = thrown()
        ex.message.contains('did you mean: a = "b"')
    }

    def "syntax validation for setting configs equal to blocks"(){
        setup:
        String config = """
        a = b{
            field = true
        }
        """

        when:
        dsl.parse(config)

        then:
        thrown(TemplateConfigException)
    }

    def "array lists are appropriately serialized"(){
        setup:
        String config = "field = [ 'a', 'b-c', 'c d' ]"
        Map expectedConfig = [
            field: [ "a", "b-c", 'c d' ]
        ]
        def originalConfig, reparsedConfig

        when:
        originalConfig = dsl.parse(config)
        reparsedConfig = dsl.parse(dsl.serialize(originalConfig))

        then:
        originalConfig.config == expectedConfig
        reparsedConfig.config == expectedConfig
    }

    def "maps keys are appropriately serialized"(){
        setup:
        String config = "field = [ 'a': 'String a', 'b-c' : 'String b-c', 'c d' : 'String c d' ]"
        Map expectedConfig = [
                field: [ 'a': 'String a', 'b-c' : 'String b-c', 'c d' : 'String c d' ]
        ]
        def originalConfig, reparsedConfig, serializeText

        when:
        originalConfig = dsl.parse(config)
        serializeText = dsl.serialize(originalConfig)
        reparsedConfig = dsl.parse(serializeText)

        then:
        serializeText.contains("[")
        originalConfig.config == expectedConfig
        reparsedConfig.config == expectedConfig
    }

    def "maps and blocks are appropriately serialized"(){
        setup:
        String config = """field = [ 'a': 'String a', 'b-c' : 'String b-c', 'c d' : 'String c d' ]
keywords{
   dev = "/[Dd]ev[elop|eloper]?/"
}
"""
        Map expectedConfig = [
                field: [ 'a': 'String a', 'b-c' : 'String b-c', 'c d' : 'String c d' ],
                keywords: [ 'dev': "/[Dd]ev[elop|eloper]?/" ]
        ]
        def originalConfig, reparsedConfig, serializeText

        when:
        originalConfig = dsl.parse(config)
        serializeText = dsl.serialize(originalConfig)
        reparsedConfig = dsl.parse(serializeText)

        then:
        serializeText.contains("[")
        serializeText.contains("{")
        originalConfig.config == expectedConfig
        reparsedConfig.config == expectedConfig
    }

    def "Double Quote String block keys with hyphens are appropriately serialized"(){
        setup:
        String config = "\"some-block\"{}"
        Map expectedConfig = [ "some-block": [:] ]
        def originalConfig, reparsedConfig

        when:
        originalConfig = dsl.parse(config)
        reparsedConfig = dsl.parse(dsl.serialize(originalConfig))

        then:
        originalConfig.config == expectedConfig
        reparsedConfig.config == expectedConfig
    }

    def "Single Quote String block keys with hyphens are appropriately serialized"(){
        setup:
        String config = "'some-block'{}"
        Map expectedConfig = [ "some-block": [:] ]
        def originalConfig, reparsedConfig

        when:
        originalConfig = dsl.parse(config)
        reparsedConfig = dsl.parse(dsl.serialize(originalConfig))

        then:
        originalConfig.config == expectedConfig
        reparsedConfig.config == expectedConfig
    }

}
