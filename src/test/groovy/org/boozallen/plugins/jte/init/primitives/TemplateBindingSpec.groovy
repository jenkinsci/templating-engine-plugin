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
package org.boozallen.plugins.jte.init.primitives

import hudson.model.Result
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.init.primitives.injectors.ApplicationEnvironment
import org.boozallen.plugins.jte.init.primitives.injectors.Keyword
import org.boozallen.plugins.jte.init.primitives.injectors.Stage
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapper
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class TemplateBindingSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    void cleanup() {
        TestLibraryProvider.wipeAllLibrarySources()
    }

    /****************************
     * Keyword overriding
     ****************************/
    def "Overriding a keyword in the binding from a template throws exception"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: ' keywords{ x = true }',
            template: 'x = false'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'x'", run)
    }
    def "Overriding a keyword in the binding from a library step throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', 'void call(){ x = false }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
                libraries{ exampleLibrary }
                keywords{ x = true }
            ''',
            template: 'someStep()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'x'", run)
    }
    def "Access an overloaded keyword results in exception"() {
        given:
        Keyword keyword = Spy()
        when:
        keyword.getValue(null)
        then:
        1 * keyword.isOverloaded()
    }

    /****************************
     * Step overriding
     ****************************/
    def "Overriding a step in the binding from a template throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', 'void call(){ x = false }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'someStep = false '
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'someStep'", run)
    }
    def "Overriding a step in the binding from a library step throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', 'void call(){}')
        libProvider.addStep('exampleLibrary', 'x', 'void call(){ someStep = false }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'x()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'someStep'", run)
    }

    def "Access an overloaded step results in exception"() {
        given:
        StepWrapper step = Spy()
        when:
        step.getValue(null)
        then:
        thrown(IllegalStateException) // CpsThread not present. doesn't matter for this test.
        1 * step.isOverloaded()
    }

    def "Invoking overloaded step via namespace works when permissive_initialization is true"(){
        given:
        CpsVmExecutorService.FAIL_ON_MISMATCH = false // needed for unit test.
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('maven', 'build', 'void call(){ println "build from maven" }')
        libProvider.addStep('gradle', 'build', 'void call(){ println "build from gradle" }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
              maven
              gradle
            }
            jte{
              permissive_initialization = true
            }
            ''',
            template: '''
            jte.libraries.maven.build()
            jte.libraries.gradle.build()
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.SUCCESS, run)
        jenkins.assertLogContains("build from maven", run)
        jenkins.assertLogContains("build from gradle", run)
    }

    /****************************
     * Stage overriding
     ****************************/
    def "Overriding a stage in the binding from a template throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', 'void call(){ x = false }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
                libraries{ exampleLibrary }
                stages{ ci{ someStep } }
            ''',
                template: 'ci = false'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'ci'", run)
    }
    def "Overriding a stage in the binding from a library step throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', 'void call(){ ci = false }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
                libraries{ exampleLibrary }
                stages{ ci{ someStep } }
            ''',
                template: 'someStep()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'ci'", run)
    }
    def "Access an overloaded stage results in exception"() {
        given:
        Stage stage = Spy()
        when:
        stage.getValue(null)
        then:
        1 * stage.isOverloaded()
    }

    /****************************
     * Application Environment overriding
     ****************************/
    def "Overriding a application environment in the binding from a template throws exception"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'application_environments{ dev }',
            template: 'dev = false'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'dev'", run)
    }
    def "Overriding a application environment in the binding from a library step throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', '''
        void call(){
            dev = false
        }
        ''')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
                libraries{ exampleLibrary }
                application_environments{ dev }
            ''',
                template: 'someStep()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'dev'", run)
    }
    def "Access an overloaded application environment results in exception"() {
        given:
        ApplicationEnvironment appEnv = Spy()
        when:
        appEnv.getValue(null)
        then:
        1 * appEnv.isOverloaded()
    }

    /****************************
     * Reserved Variable Name overriding
     ****************************/
    def "Overriding a reserved variable name in the binding from a template throws exception"() {
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            template: 'hookContext = false'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'hookContext'", run)
    }
    def "Overriding a reserved variable name in the binding from a library step throws exception"() {
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('exampleLibrary', 'someStep', 'void call(){ hookContext = false }')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'someStep()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'hookContext'", run)
    }

}
