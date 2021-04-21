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
package org.boozallen.plugins.jte.util

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * to be mocked so Spock can find "run" method
 */
class ConfigValidatorSpec extends Specification {

    @ClassRule @Shared JenkinsRule jenkins = new JenkinsRule()
    @Shared ConfigValidator validator

    def setupSpec(){
        WorkflowJob job = jenkins.createProject(WorkflowJob)
        job.setDefinition(new CpsFlowDefinition("println 'hi'"))
        job.scheduleBuild2(0).waitForStart()
        WorkflowRun r = job.getLastBuild()
        jenkins.waitForCompletion(r)
        FlowExecutionOwner flowOwner = r.asFlowExecutionOwner()
        validator = new ConfigValidator(flowOwner)
    }

    def "invalid required key throws exception"() {
        setup:
        String schema = 'fields{ required{ example = String } }'
        LinkedHashMap config = [ example: 11 ]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.getMessage().contains("Field 'example' must be a String")
    }

    def "valid required key does not throw exception"() {
        setup:
        String schema = 'fields{ required{ example = String } }'
        LinkedHashMap config = [ example: 'a string' ]

        when:
        validator.validate(schema, config)

        then:
        notThrown(AggregateException)
    }

    def "missing required key throws exception"() {
        setup:
        String schema = 'fields{ required{ example = String } }'
        LinkedHashMap config = [:]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.getMessage().contains("Missing required field 'example'")
    }

    def "invalid optional key throws exception"() {
        setup:
        String schema = 'fields{ optional{ example = String } }'
        LinkedHashMap config = [ example: 11 ]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.getMessage().contains("Field 'example' must be a String")
    }

    def "missing optional key is okay"() {
        setup:
        String schema = 'fields{ optional{ example = String } }'
        LinkedHashMap config = [:]

        when:
        validator.validate(schema, config)

        then:
        notThrown(AggregateException)
    }

    def "keys not in schema throw exception"() {
        setup:
        String schema = 'fields{ required{ example = String } }'
        LinkedHashMap config = [ nope: 11 ]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.getMessage().contains("Field 'nope' is not used.")
    }

    def "nested required keys are validated"() {
        setup:
        String schema = 'fields{ required{ nested{ example = String } } }'
        LinkedHashMap config = [ nested: [ example: 11 ] ]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.getMessage().contains("Field 'nested.example' must be a String")
    }

    def "nested optional keys are validated"() {
        setup:
        String schema = 'fields{ optional{ nested{ example = String } } }'
        LinkedHashMap config = [ nested: [ example: 11 ] ]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.getMessage().contains("Field 'nested.example' must be a String")
    }

    def "aggregate exception has all errors"() {
        setup:
        String schema = '''
        fields{
            required{
                fieldA = String
                fieldB = Number
            }
            optional{
                fieldC = boolean
            }
        }'''
        LinkedHashMap config = [
            fieldA: 11,
            fieldB: 'a string',
            fieldC: 11
        ]

        when:
        validator.validate(schema, config)

        then:
        AggregateException ex = thrown()
        ex.size() == 3
        ex.getMessage().contains("Field 'fieldA' must be a String")
        ex.getMessage().contains("Field 'fieldB' must be a Number")
        ex.getMessage().contains("Field 'fieldC' must be a boolean")
    }

    @Unroll
    def "when config value is '#actual' and expected type/value is #expected then result is #result"() {
        expect:
        validator.validateType(Mock(TemplateLogger), actual, expected) == result

        where:
        actual      |     expected      | result
        true        |      boolean      | false
        false       |      boolean      | false
        true        |      Boolean      | false
        false       |      Boolean      | false
        'nope'      |      boolean      | true
        'hey'       |      String       | false
        "${4}"      |      String       | false
        4           |      String       | true
        4           |      Integer      | false
        4           |      int          | false
        4.2         |      Integer      | true
        4.2         |      int          | true
        1           |      Double       | true
        1.0         |      Double       | false
        1           |      Number       | false
        1.0         |      Number       | false
        'hey'       |     ~/.*he.*/     | false
        'heyyy'     |     ~/^hey.*/     | false
        'hi'        |     ~/^hey.*/     | true
        'hi'        |    ['hi', 'hey']  | false
        'opt3'      |  ['opt1', 'opt2'] | true
        [ 'a' ]     |       List        | false
        'a'         |       List        | true
        [ 'a' ]     |  ArrayList        | false
        'a'         |  ArrayList        | true
    }

}
