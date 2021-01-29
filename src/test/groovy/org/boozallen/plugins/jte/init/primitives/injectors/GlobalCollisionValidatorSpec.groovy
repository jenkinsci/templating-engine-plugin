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
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.model.Result
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class GlobalCollisionValidatorSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def setupSpec(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("gradle", "build", """
        void call(){
            println "build step from test gradle library"
        }
        """)

        libProvider.addGlobally()
    }

    def "library step collides with 'build' logs message"(){
        def run
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            libraries{
                gradle
            }

            """, template: "build()")

        expect:
        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.SUCCESS, run)
        jenkins.assertLogContains(GlobalCollisionValidator.warningHeading, run)
        jenkins.assertLogContains("build", run)
    }

}
