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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class KeywordSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def "injector inserts keyword into binding"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'keywords{ x = 11 }',
            template: 'assert x == 11'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "retrieving keyword from binding results in value"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'keywords{ x = "foo" }',
            template: 'assert x == "foo"'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "retrieving namespaced keyword from binding results in value"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'keywords{ x = "foo" }',
                template: 'assert jte.keywords.x == "foo"'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "inject multiple keywords"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            keywords{
                x = "foo"
                y = "bar"
            }
            ''',
            template: '''
            assert x == "foo"
            assert y == "bar"
            '''
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "inject multiple keywords namespaced and not"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: '''
            keywords{
                x = "foo"
                y = "bar"
            }
            ''', template: '''
            assert x == "foo"
            assert jte.keywords.y == "bar"
            '''
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "override during initialization throws error"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            keywords{
                x = "foo"
            }

            application_environments{
                x
            }
            ''',
            template: 'println x'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "override post initialization throws error"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            keywords{
                x = "foo"
            }
            ''',
            template: 'x = "oops"'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "getParentChain returns the correct path"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "keywords{ x = 'foo' }",
            template: "assert x == 'foo'"
        )
        WorkflowRun run = job.scheduleBuild2(0).waitForStart()
        jenkins.waitForCompletion(run)
        TemplatePrimitiveCollector c = run.getAction(TemplatePrimitiveCollector)
        Keyword keyword = c.findAll { primitive ->
            primitive.getName() == 'x'
        }.first()

        expect:
        jenkins.assertBuildStatusSuccess(run)
        keyword.getParentChain() == 'jte.keywords.x'
    }

}
