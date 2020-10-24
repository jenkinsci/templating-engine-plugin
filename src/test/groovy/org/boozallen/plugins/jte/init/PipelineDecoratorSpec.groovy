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
package org.boozallen.plugins.jte.init.primitives

import org.boozallen.plugins.jte.init.PipelineDecorator
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PipelineDecoratorSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    @WithoutJenkins
    def "retrieve jte.allow_scm_jenkinsfile"(){
        when:
        def jte = new PipelineDecorator.JteBlockWrapper( [allow_scm_jenkinsfile:false])

        then:
        jte.allow_scm_jenkinsfile == false
    }

    @WithoutJenkins
    def "retrieve default true for jte.allow_scm_jenkinsfile when jte block is empty"(){
        when:
        def jte = new PipelineDecorator.JteBlockWrapper([:])

        then:
        jte.allow_scm_jenkinsfile == true
    }

    @WithoutJenkins
    def "throws exception on extra config"(){
        when:
        new PipelineDecorator.JteBlockWrapper([extra:true])

        then:
        thrown(MissingPropertyException)
    }

    @WithoutJenkins
    @Unroll
    def "when config value is '#config' and expected allow_scm_jenkinsfile is #expected_allow and  jte.pipeline_template is #expected_template\n"() {
        when:
        def jte = new PipelineDecorator.JteBlockWrapper(config)

        then:
        jte.allow_scm_jenkinsfile == expected_allow
        jte.pipeline_template == expected_template

        where:
        config | expected_allow | expected_template
        [:]   | true        | null
        [allow_scm_jenkinsfile:false] | false | null
        [allow_scm_jenkinsfile:false, pipeline_template:'dev_template'] | false | 'dev_template'
    }

}
