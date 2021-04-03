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
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class StageSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def setupSpec() {
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('gradle', 'build', '''
        void call(){
            println "build step from test gradle library"
        }
        ''')
        libProvider.addStep('gradle', 'unit_test', '''
        void call(){
            println "unit_test step from test gradle library"
        }
        ''')
        libProvider.addStep('gradle', 'printStageArgs', """
        void call(){
            println "x=\${stageContext.args.x}"
        }
        """)
        libProvider.addGlobally()
    }

    def "validate stage executes single step"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    build
                }
            }
            ''',
            template: 'ci()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('build step from test gradle library', run)
    }

    def "validate stage logs its entrypoint"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    build
                }
            }
            ''',
            template: 'ci()'
        )

        expect:
        jenkins.assertLogContains('[Stage - ci]', job.scheduleBuild2(0).get())
    }

    def "validate stage executes multiple steps"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    build
                    unit_test
                }
            }
            ''',
            template: 'ci()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        TestUtil.assertOrder(jenkins.getLog(run), [
            'build step from test gradle library',
            'unit_test step from test gradle library'
        ])
    }

    def "validate stage arguments are passed through to steps"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    printStageArgs
                }
            }
            ''',
            template: 'ci(x: "foo")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
    }

    def "validate namespaced stage arguments are passed through to steps"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            libraries{
                gradle
            }

            stages{
                ci{
                    printStageArgs
                }
            }
            """, template: 'jte.stages.ci(x: "foo")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
    }

    def "validate override during initialization throws exception"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    build
                }
            }

            keywords{
                ci = "oops"
            }
            ''',
            template: 'ci()'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "validate override post initialization throws exception"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    build
                }
            }
            ''',
            template: 'ci = "oops"'
        )

        expect:
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))
    }

    def "getParentChain returns the correct path"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                gradle
            }

            stages{
                ci{
                    build
                }
            }
            ''',
            template: 'println "doesnt matter"'
        )
        WorkflowRun run = job.scheduleBuild2(0).waitForStart()
        jenkins.waitForCompletion(run)
        TemplatePrimitiveCollector c = run.getAction(TemplatePrimitiveCollector)
        Stage stage = c.findAll { primitive ->
            primitive.getName() == 'ci'
        }.first()

        expect:
        jenkins.assertBuildStatusSuccess(run)
        stage.getParentChain() == 'jte.stages.ci'
    }

}
