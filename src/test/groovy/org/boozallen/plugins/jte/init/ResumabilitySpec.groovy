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

import org.boozallen.plugins.jte.init.PipelineDecorator
import org.junit.Rule
import org.jvnet.hudson.test.RestartableJenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import org.junit.runners.model.Statement
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep

class ResumabilitySpec extends Specification{

    @Rule RestartableJenkinsRule story = new RestartableJenkinsRule()

    def "Pipeline resumes after graceful restart"(){
        when: 
        story.then({ jenkins -> 
            WorkflowJob p = TestUtil.createAdHoc(
                template: """
                println "running before sleep"
                semaphore "wait"
                println "running after sleep"
                """, jenkins, "p"
            )
            WorkflowRun b = p.scheduleBuild2(0).waitForStart()
            SemaphoreStep.waitForStart("wait/1", b)
        })

        then: 
        story.then({ jenkins -> 
            SemaphoreStep.success("wait/1", true)
            WorkflowJob p = jenkins.getInstance().getItemByFullName("p", WorkflowJob);
            WorkflowRun b = p.getLastBuild()
            jenkins.waitForCompletion(b)
            jenkins.assertLogContains("running after sleep", b);
        })
    }

    def "Stages succeed after pipeline graceful restart"(){}
    def "Steps succeed after pipeline graceful restart"(){}

}