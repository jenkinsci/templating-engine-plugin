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
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

class StepAliasSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def "Empty @StepAlias results in warning logged"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', '@StepAlias void call(){}')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: 'println "doesnt matter"'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('@StepAlias did not define any aliases for the alias\'s npm_invoke step', run)
    }
    def "Just string parameter to @StepAlias"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias("build")
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ alias }',
                template: 'build()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('running the step', run)
    }
    def "Array of Strings to @StepAlias"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(["build", "unit_test"])
        void call(){
            return "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: '''
            println "build: ${build()}"
            println "unit_test: ${unit_test()}"
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('build: running the step', run)
        jenkins.assertLogContains('unit_test: running the step', run)
    }
    def "Dynamic @StepAlias returning string"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = { return 'build' })
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: 'build()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('running the step', run)
    }
    def "Dynamic @StepAlias returning GStringImpl"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = { def x = "test"; return "unit_\${x}" })
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: 'unit_test()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('running the step', run)
    }
    def "Dynamic @StepAlias returning array of String"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = { return ["build", "unit_test"] })
        void call(){
            return "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: '''
            println "build: ${build()}"
            println "unit_test: ${unit_test()}"
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('build: running the step', run)
        jenkins.assertLogContains('unit_test: running the step', run)
    }
    def "Dynamic @StepAlias returning non-string object throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = { return 11 })
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: 'build()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains('@StepAlias Dynamic closure must return a string, received:', run)
    }
    def "Dynamic @StepAlias returning array with strings and non-strings"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = { return [ "build", 11 ] })
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: 'build()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains('@StepAlias Dynamic closure returned a collection with non-string element:', run)
    }
    def "Dynamic @StepAlias returning null is ignored"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(value = "build", dynamic = { return null })
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ alias }',
            template: 'build()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('running the step', run)
    }
    def "Dynamic @StepAlias not a closure throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = 11)
        void call(){
            println "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ alias }',
                template: 'build()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
    }
    def "Dynamic @StepAlias can resolve library configuration"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(dynamic = { return config.stepAliases })
        void call(){
            return "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias{
                stepAliases = [ "build", "unit_test" ]
              }
            }''',
            template: '''
                println "build: ${build()}"
                println "unit_test: ${unit_test()}"
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('build: running the step', run)
        jenkins.assertLogContains('unit_test: running the step', run)
    }
    def "Providing both value and dynamic"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(value = "build", dynamic = { return "unit_test" })
        void call(){
            return "running the step"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            println "build: ${build()}"
            println "unit_test: ${unit_test()}"
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('build: running the step', run)
        jenkins.assertLogContains('unit_test: running the step', run)
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/259")
    def "default parameters on StepAlias method works as expected"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(["build", "unit_test"])
        void call(param = []){
            return "running the step"
        }
        """
        )
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
        config: '''
            libraries{
              alias
            }''',
        template: '''
            println "build: ${build()}"
            println "unit_test: ${unit_test()}"
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    def "StepAliases across methods work as expected"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(["build"])
        void call(){
            println "running the \${stepContext.name} step"
        }

        @StepAlias(["unit_test"])
        void other(){}
        """
        )
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
        config: '''
        libraries{
          alias
        }''',
        template: '''
        build()
        unit_test()
        '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running the build step", run)
        jenkins.assertLogContains("running the unit_test step", run)
    }

    def "StepContext name is resolvable in an aliased step"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(value = "build", dynamic = { return "unit_test" })
        void call(){
            println "running as \${stepContext.name}"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            build()
            unit_test()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('running as build', run)
        jenkins.assertLogContains('running as unit_test', run)
    }
    def "StepContext name is resolvable in a non-aliased step"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        void call(){
            println "running as \${stepContext.name}"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            npm_invoke()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('running as npm_invoke', run)
    }
    def "StepContext isAlias is true for aliased step"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias("build")
        void call(){
            assert stepContext.isAlias
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            build()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }
    def "isAlias is false for non-aliased step"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        void call(){
            assert !stepContext.isAlias
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            npm_invoke()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }
    def "@StepAlias keepOriginal does not create step when false"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias("build")
        void call(){
            assert isAlias
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            npm_invoke()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
    }
    def "@StepAlias keepOriginal does create step when true"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(value = "build", keepOriginal = true)
        void call(){
            println "whatever"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }''',
            template: '''
            build()
            npm_invoke()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }
    def "Example use case works as expected"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('npm', 'npm_invoke', """
        @StepAlias(dynamic = { return config.phases.keySet() })
        void call(){
            def phaseConfig = config.phases[stepContext.name]
            println "running as \${stepContext.name}, script target: \${phaseConfig.script}"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              npm{
                phases{
                  build{
                    script = "package"
                  }
                  unit_test{
                    script = "test"
                  }
                }
              }
            }''',
            template: '''
            build()
            unit_test()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running as build, script target: package", run)
        jenkins.assertLogContains("running as unit_test, script target: test", run)
    }
    def "Hook triggered after aliased step run during stage"(){
        TestLibraryProvider libProvider = new TestLibraryProvider()

        libProvider.addStep('alias', 'npm_invoke', """
        @StepAlias(value = "build")
        void call(){
            println "build step"
        }"""
        )

        libProvider.addStep('alias', 'hookStep', """
        @AfterStep
        void call(){
            println "running after \${hookContext.step}"
        }"""
        )

        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              alias
            }
            stages{
              ci{
                build
              }
            }
            ''',
            template: '''
            ci()
        '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("build step", run)
        jenkins.assertLogContains("running after build", run)
    }

    void cleanup(){
        TestLibraryProvider.removeLibrarySources()
    }

}
