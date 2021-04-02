package org.boozallen.plugins.jte.init.primitives

import hudson.model.Result
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class TemplateBindingSpec extends Specification{

    @Rule JenkinsRule jenkins = new JenkinsRule()

    /****************************
     * Keyword overriding
     ****************************/
    def "Overriding a keyword in the binding from a template throws exception"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: " keywords{ x = true }",
            template: "x = false"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'x'", run)
    }

    def "Overriding a keyword in the binding from a library step throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", "void call(){ x = false }")
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
                libraries{ exampleLibrary }
                keywords{ x = true }
            """,
            template: "someStep()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'x'", run)
    }

    /****************************
     * Step overriding
     ****************************/
    def "Overriding a step in the binding from a template throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", "void call(){ x = false }")
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "someStep = false "
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'someStep'", run)
    }

    def "Overriding a step in the binding from a library step throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", "void call(){}")
        libProvider.addStep("exampleLibrary", "x", "void call(){ someStep = false }")
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "x()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'someStep'", run)
    }

    /****************************
     * Stage overriding
     ****************************/
    def "Overriding a stage in the binding from a template throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", "void call(){ x = false }")
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
                libraries{ exampleLibrary }
                stages{ ci{ someStep } }
            """,
                template: "ci = false"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'ci'", run)
    }
    def "Overriding a stage in the binding from a library step throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", "void call(){ ci = false }")
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
                libraries{ exampleLibrary }
                stages{ ci{ someStep } }
            """,
                template: "someStep()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'ci'", run)
    }

    /****************************
     * Application Environment overriding
     ****************************/
    def "Overriding a application environment in the binding from a template throws exception"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "application_environments{ dev }",
            template: "dev = false"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'dev'", run)
    }

    def "Overriding a application environment in the binding from a library step throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", """
        void call(){ 
            dev = false 
        }
        """)
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
                libraries{ exampleLibrary }
                application_environments{ dev }
            """,
                template: "someStep()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'dev'", run)
    }

    /****************************
     * Reserved Variable Name overriding
     ****************************/
    def "Overriding a reserved variable name in the binding from a template throws exception"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            template: "hookContext = false"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'hookContext'", run)
    }

    def "Overriding a reserved variable name in the binding from a library step throws exception"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "someStep", "void call(){ hookContext = false }")
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "someStep()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Failed to set variable 'hookContext'", run)
    }

}
