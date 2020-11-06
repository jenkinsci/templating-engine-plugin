/*
    Copyright 2020 Booz Allen Hamilton

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
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.model.Result
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class JteBlockValidatorSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def "empty jte block succeeds"(){
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: "jte{}",
                template: """
                assert 1 == 1
                """
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "non-existent jte block succeeds"(){
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                template: """
                assert 1 == 1
                """
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "jte block with boolean allow_scm_jenkins succeeds"(){
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: """jte{
                    allow_scm_jenkinsfile = false
                    pipeline_template = "my_template"
                }
                """,
                template: """
                assert 1 == 1
                """
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "jte block with wrong field fails"(){
        def run
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: """jte{
                    bad_field = "false"
                    allow_scm_jenkinsfile = false
                    pipeline_template = "my_template"
                }
                """,
                template: """
                assert 1 == 1
                """
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains(JteBlockValidator.ERROR_HEADER, run)
        jenkins.assertLogContains("bad_field", run)
    }

    def "jte block with wrong type fails"(){
        def run
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: """jte{
                    allow_scm_jenkinsfile = 0
                    pipeline_template = "my_template"
                }
                """,
                template: """
                assert 1 == 1
                """
        )

        expect:
        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains(JteBlockValidator.ERROR_HEADER, run)
        jenkins.assertLogContains("pipeline_template", run)
    }

}
