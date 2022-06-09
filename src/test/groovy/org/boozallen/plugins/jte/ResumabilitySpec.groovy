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

import org.junit.Rule
import org.jvnet.hudson.test.RestartableJenkinsRule
import spock.lang.Issue
import spock.lang.Specification
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.boozallen.plugins.jte.util.TestUtil
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep

class ResumabilitySpec extends Specification {

    @Rule RestartableJenkinsRule story = new RestartableJenkinsRule()

    def setup() {
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('gradle', 'build', '''
        void call(){
            println "build step from test gradle library"
            node{
                sh "echo hi"
            }
        }
        ''')
        libProvider.addStep('gradle', 'sleepInStep', '''
        void call(){
            println "running before sleep"
            semaphore "wait"
            println "running after sleep"
        }''')
        story.then { jenkins -> libProvider.addGlobally() }
    }

    def "Pipeline resumes after graceful restart"() {
        when:
        WorkflowJob job
        story.then { jenkins ->
            job = TestUtil.createAdHoc(jenkins,
                template: '''
                println "running before sleep"
                semaphore "wait"
                println "running after sleep"
                '''
            )
            WorkflowRun run = job.scheduleBuild2(0).waitForStart()
            SemaphoreStep.waitForStart('wait/1', run)
        }

        then:
        story.then { jenkins ->
            SemaphoreStep.success('wait/1', true)
            job = jenkins.getInstance().getItemByFullName(job.getName(), WorkflowJob)
            WorkflowRun run = job.getLastBuild()
            jenkins.waitForCompletion(run)
            jenkins.assertBuildStatusSuccess(run)
            jenkins.assertLogContains('running after sleep', run)
        }
    }

    def "Stages succeed after pipeline graceful restart"() {
        when:
        WorkflowJob job
        story.then { jenkins ->
            job = TestUtil.createAdHoc(jenkins,
                config: '''
                libraries{ gradle }
                stages{ ci{ build } }
                ''',
                template: '''
                println "running before sleep"
                semaphore "wait"
                println "running after sleep"
                ci()
                '''
            )
            WorkflowRun run = job.scheduleBuild2(0).waitForStart()
            SemaphoreStep.waitForStart('wait/1', run)
        }

        then:
        story.then { jenkins ->
            SemaphoreStep.success('wait/1', true)
            job = jenkins.getInstance().getItemByFullName(job.getName(), WorkflowJob)
            WorkflowRun run = job.getLastBuild()
            jenkins.waitForCompletion(run)
            jenkins.assertBuildStatusSuccess(run)
            jenkins.assertLogContains('build step from test gradle library', run)
        }
    }

    def "Steps succeed after pipeline graceful restart"() {
        when:
        WorkflowJob job
        story.then { jenkins ->
            job = TestUtil.createAdHoc(jenkins,
                config: '''
                libraries{ gradle }
                stages{ ci{ build } }
                ''',
                template: '''
                println "running before sleep"
                semaphore "wait"
                build()
                '''
            )
            WorkflowRun run = job.scheduleBuild2(0).waitForStart()
            SemaphoreStep.waitForStart('wait/1', run)
        }

        then:
        story.then { jenkins ->
            SemaphoreStep.success('wait/1', true)
            job = jenkins.getInstance().getItemByFullName(job.getName(), WorkflowJob)
            WorkflowRun run = job.getLastBuild()
            jenkins.waitForCompletion(run)
            jenkins.assertBuildStatusSuccess(run)
            jenkins.assertLogContains('build step from test gradle library', run)
        }
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/191")
    def "Restart mid-step resumes successfully"() {
        when:
        WorkflowJob job
        story.then { jenkins ->
            job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ gradle }',
                template: 'sleepInStep()'
            )
            WorkflowRun run = job.scheduleBuild2(0).waitForStart()
            SemaphoreStep.waitForStart('wait/1', run)
        }

        then:
        story.then { jenkins ->
            SemaphoreStep.success('wait/1', true)
            job = jenkins.getInstance().getItemByFullName(job.getName(), WorkflowJob)
            WorkflowRun run = job.getLastBuild()
            jenkins.waitForCompletion(run)
            jenkins.assertBuildStatusSuccess(run)
            jenkins.assertLogContains('running after sleep', run)
        }
    }

}
