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

class ApplicationEnvironmentSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def "Injector populates binding"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'application_environments{ dev{ someField = true } }',
            template: 'assert dev.someField'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "default short_name is environment key"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'application_environments{ dev }',
            template: 'assert dev.short_name == "dev"'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "set short_name"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev{
                    short_name = "Dev"
                }
            }
            ''',
            template: 'assert dev.short_name == "Dev"'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "default long_name is environment key"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
            }
            ''',
            template: 'assert dev.long_name == "dev"'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "set long_name"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev{
                    long_name = "Development"
                }
            }
            ''',
            template: 'assert dev.long_name == "Development"'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "can set arbitrary additional key"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev{
                    extra1 = "foo"
                    extra2 = "bar"
                }
            }
            ''',
            template: '''
            assert dev.extra1 == "foo"
            assert dev.extra2 == "bar"
            '''
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "missing property returns null"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
            }
            ''',
            template: 'assert dev.extra1 == null'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "application environments are immutable"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev{
                    extra = "foo"
                }
            }
            ''',
            template: 'dev.extra = "bar"'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "can set multiple environments"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev{ field = 1 }
                test{ field = 2 }
                prod{ field = 3 }
            }
            ''',
            template: '''
            assert dev.field == 1
            assert test.field == 2
            assert prod.field == 3
            '''
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "fail on override during initialization"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }

            keywords{
                dev = "oops"
            }
            ''',
            template: 'assert binding.hasVariable("dev")'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "fail on override post initialization"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
            }
            ''',
            template: 'dev = "oops"'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "first environment's previous is null"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: 'assert dev.previous == null'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "second env's previous is correct"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: 'assert test.previous == dev'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "first env's next is correct"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: 'assert dev.next == test'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "when only one environment previous/next are null"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
            }
            ''',
            template: '''
            assert dev.previous == null
            assert dev.next == null
            '''
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "when >= 3 envs, middle envs previous and next are correct"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: '''
            assert test.previous == dev
            assert test.next == prod
            '''
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "last environment's next is null"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: 'assert prod.next == null'
        )

        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "defining the previous configuration throws exception"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: 'test.previous = "oops"'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "defining the next configuration throws exception"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: 'assert test.next = "oops"'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "getParentChain returns the correct path"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
            }
            ''',
            template: 'println "doesnt matter"'
        )
        WorkflowRun run = job.scheduleBuild2(0).waitForStart()
        jenkins.waitForCompletion(run)
        TemplatePrimitiveCollector c = run.getAction(TemplatePrimitiveCollector)
        ApplicationEnvironment appEnv = c.findAll { primitive ->
            primitive.getName() == 'dev'
        }.first()

        expect:
        jenkins.assertBuildStatusSuccess(run)
        appEnv.getParentChain() == 'jte.application_environments.dev'
    }

    def "ApplicationEnvironment can be accessed via namespace"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev
                test
                prod
            }
            ''',
            template: '''
            assert jte.application_environments.dev
            assert jte.application_environments.test
            assert jte.application_environments.prod
            '''
        )
        expect:
        jenkins.buildAndAssertSuccess(job)
    }

    def "ApplicationEnvironment custom field can be accessed via namespace"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            application_environments{
                dev{
                    custom_field = "banana"
                }
            }
            ''',
            template: '''
            assert jte.application_environments.dev.custom_field == "banana"
            '''
        )
        expect:
        jenkins.buildAndAssertSuccess(job)
    }

}
