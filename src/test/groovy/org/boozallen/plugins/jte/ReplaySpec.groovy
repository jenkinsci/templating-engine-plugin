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
        WorkflowJob p = TestUtil.createAdHoc(
                config: 'keywords{ message = "hello world" }',
                template: template, jenkins, 'p'
        )
        then:
        WorkflowRun b1 = jenkins.assertBuildStatusSuccess(p.scheduleBuild2(0))
        jenkins.assertLogContains("hello world", b1)
        then:
        WorkflowRun b2 = b1.getAction(ReplayAction).run(template, [:]).get()
        jenkins.assertBuildStatusSuccess(b2)
        jenkins.assertLogContains("hello world")

    }
}
