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
package org.boozallen.plugins.jte

import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.cps.replay.ReplayAction
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

class ReplaySpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/222")
    def "replay declarative pipeline and access keyword"(){
        when:
        String template = '''
        pipeline{
          agent any
          stages{
            stage("stage"){
              steps{
                echo message
              }
            }
          }
        }
        '''
        WorkflowJob p = TestUtil.createAdHoc(jenkins,
                config: 'keywords{ message = "hello world" }',
                template: template
        )
        then:
        WorkflowRun b1 = jenkins.assertBuildStatusSuccess(p.scheduleBuild2(0))
        jenkins.assertLogContains("hello world", b1)
        then:
        WorkflowRun b2 = b1.getAction(ReplayAction).run(template, [:]).get()
        jenkins.assertBuildStatusSuccess(b2)
        jenkins.assertLogContains("hello world", b2)
    }

}
