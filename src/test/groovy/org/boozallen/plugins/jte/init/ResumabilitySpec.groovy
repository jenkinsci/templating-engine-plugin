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
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;


class ResumabilitySpec extends Specification{

    @Rule RestartableJenkinsRule story = new RestartableJenkinsRule()

    def "Pipeline resumes after graceful restart"(){
        when: 
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition("""
                println "running before sleep"
                sleep 15
                println "running after sleep"
                """))
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                // SemaphoreStep.waitForStart("wait/1", b);
                story.j.waitForMessage("running before sleep", b);
            }
        });

        then: 
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                // SemaphoreStep.success("wait/1", null);
                WorkflowJob p = story.j.jenkins.getItemByFullName("p", WorkflowJob.class);
                WorkflowRun b = p.getLastBuild();
                story.j.assertLogContains("running after sleep", story.j.waitForCompletion(b));
            }
        });
    }

    def "Stages succeed after pipeline graceful restart"(){}
    def "Steps succeed after pipeline graceful restart"(){}

}