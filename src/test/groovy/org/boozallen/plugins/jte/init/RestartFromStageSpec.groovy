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
package org.boozallen.plugins.jte.init

import hudson.model.Action
import hudson.model.Cause
import hudson.model.CauseAction
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.RestartFlowFactoryAction
import org.jenkinsci.plugins.pipeline.modeldefinition.causes.RestartDeclarativePipelineCause
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class RestartFromStageSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def setup() {
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('resumeTester', 'createStash', '''
        void call() {
            node {
                def tempdir="${pwd(tmp: true)}/${BUILD_ID}/stashtest"
                dir(tempdir) {
                    writeFile file: 'stashtest.txt', text: "Hi I was stashed"
                    stash name: "jte-stash-test", includes: "stashtest.txt"
                }
            }
        }
        ''')
        libProvider.addStep('resumeTester', 'useStash', '''
        void call(){
            node {
                def tempdir="${pwd(tmp: true)}/${BUILD_ID}/stashtest"
                dir(tempdir) {
                    unstash name: "jte-stash-test"
                    def contents=readFile file: 'stashtest.txt\'
                    echo "contents:${contents}"
                }
            }
        }
        ''')
        libProvider.addGlobally()
    }

    def "Restart from stage has stashes when not using steps"() {
        given:
        String pipeline = '''
        pipeline {
          agent none
          options {
            preserveStashes(buildCount: 5)
          }
          stages {
            stage('One') {
              steps {
                script{
                  node {
                    def tempdir="${pwd(tmp: true)}/${BUILD_ID}/stashtest"
                    dir(tempdir) {
                      writeFile file: 'stashtest.txt', text: "Hi I was stashed"
                      stash name: "jte-stash-test", includes: "stashtest.txt"
                    }
                  }
                }
              }
            }
            stage('Two') {
              steps {
                script{
                  node {
                    def tempdir="${pwd(tmp: true)}/${BUILD_ID}/stashtest"
                    dir(tempdir) {
                      unstash name: "jte-stash-test"
                      def contents=readFile file: 'stashtest.txt'
                      echo "contents:${contents}"
                    }
                  }
                }
              }
            }
          }
        }
        '''

        WorkflowJob p = TestUtil.createAdHoc(
            config: 'libraries {resumeTester}',
            template: pipeline,
            jenkins
        )

        WorkflowRun b = p.scheduleBuild2(0).waitForStart()
        jenkins.waitForCompletion(b)
        jenkins.assertBuildStatusSuccess(b)

        when:
        List<Action> actions = []
        actions.add(new RestartFlowFactoryAction(b.getExternalizableId()))
        actions.add(new CauseAction(new Cause.UserIdCause(), new RestartDeclarativePipelineCause(b, 'Two')))
        WorkflowRun b2 = p.scheduleBuild2(0, actions.toArray(new Action[actions.size()])).waitForStart()
        jenkins.waitUntilNoActivity()

        then:
        jenkins.assertBuildStatusSuccess(b2)
        jenkins.assertLogContains("[JTE] Copying loaded primitives from previous run", b2)
        jenkins.assertLogContains('Stage "One" skipped due to this build restarting at stage "Two"', b2)
        jenkins.assertLogContains('contents:Hi I was stashed', b2)
    }

    def "Restart from stage has stashes when using steps"() {
        given:
        String pipeline = '''
        pipeline {
          agent none
          options {
            preserveStashes(buildCount: 5)
          }
          stages {
            stage('One') {
              steps {
                createStash()
              }
            }
            stage('Two') {
              steps {
                useStash()
              }
            }
          }
        }
        '''

        WorkflowJob p = TestUtil.createAdHoc(
            config: 'libraries {resumeTester}',
            template: pipeline,
            jenkins
        )

        WorkflowRun b = p.scheduleBuild2(0).waitForStart()
        jenkins.waitForCompletion(b)
        jenkins.assertBuildStatusSuccess(b)

        when:
        List<Action> actions = []
        actions.add(new RestartFlowFactoryAction(b.getExternalizableId()))
        actions.add(new CauseAction(new Cause.UserIdCause(), new RestartDeclarativePipelineCause(b, 'Two')))
        WorkflowRun b2 = p.scheduleBuild2(0, actions.toArray(new Action[actions.size()])).waitForStart()
        jenkins.waitUntilNoActivity()

        then:
        jenkins.assertBuildStatusSuccess(b2)
        jenkins.assertLogContains("[JTE] Copying loaded primitives from previous run", b2)
        jenkins.assertLogContains('Stage "One" skipped due to this build restarting at stage "Two"', b2)
        jenkins.assertLogContains('contents:Hi I was stashed', b2)
    }

}
