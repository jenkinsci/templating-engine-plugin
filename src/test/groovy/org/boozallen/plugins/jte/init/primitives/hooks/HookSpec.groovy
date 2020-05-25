package org.boozallen.plugins.jte.init.primitives.hooks

import hudson.model.Result
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

class HookSpec extends Specification{

    @Rule JenkinsRule jenkins = new JenkinsRule()

    def "Hooks execute in order"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        @Validate void validate(context){ println "Validate Hook" }
        @Init void init(context){ println "Init Hook" }
        @BeforeStep void beforeStep(context){ println "BeforeStep Hook" }
        @AfterStep void afterStep(context){ println "AfterStep Hook" }
        @Notify void doNotify(context){ println "Notify Hook" }
        @CleanUp void cleanup(context){ println "CleanUp Hook" }
        """)
        libProvider.addStep("someLibrary", "aStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary
                someLibrary
            }
            """,
            template: "aStep()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatusSuccess(run)
        TestUtil.assertOrder(jenkins.getLog(run), [
            "Validate Hook",
            "Init Hook",
            "BeforeStep Hook",
            "the actual step",
            "AfterStep Hook",
            "Notify Hook",
            "CleanUp Hook", 
            "Notify Hook"
        ])
    }

    def "Hooks log their execution"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        @Validate void validate(context){ println "Validate Hook" }
        @Init void init(context){ println "Init Hook" }
        @BeforeStep void beforeStep(context){ println "BeforeStep Hook" }
        @AfterStep void afterStep(context){ println "AfterStep Hook" }
        @Notify void doNotify(context){ println "Notify Hook" }
        @CleanUp void cleanup(context){ println "CleanUp Hook" }
        """)
        libProvider.addStep("someLibrary", "aStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary
                someLibrary
            }
            """,
            template: "aStep()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("[JTE][@Validate - hooksLibrary/theHooks.validate]", run)
        jenkins.assertLogContains("[JTE][@Init - hooksLibrary/theHooks.init]", run)
        jenkins.assertLogContains("[JTE][@BeforeStep - hooksLibrary/theHooks.beforeStep]", run)
        jenkins.assertLogContains("[JTE][@AfterStep - hooksLibrary/theHooks.afterStep]", run)
        jenkins.assertLogContains("[JTE][@Notify - hooksLibrary/theHooks.doNotify]", run)
        jenkins.assertLogContains("[JTE][@CleanUp - hooksLibrary/theHooks.cleanup]", run)
        jenkins.assertLogContains("[JTE][@Notify - hooksLibrary/theHooks.doNotify]", run)
    }

    def "Hook context variable resolvable inside hook closure parameter"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        @BeforeStep({ context.step.equals("foo") })
        void blah(context){
            println "running before \${context.step}"
        }
        """)
        libProvider.addStep("someLibrary", "foo", """
        void call(){
            println "step: foo"
        }
        """)
        libProvider.addStep("someLibrary", "bar", """
        void call(){
            println "step: bar"
        }
        """)
        libProvider.addStep("someLibrary", "aStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary
                someLibrary
            }
            """,
            template: "foo(); bar()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running before foo", run)
        jenkins.assertLogNotContains("running before bar", run)
        
    }

    def "library config variable resolvable inside hook closure parameter"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        @BeforeStep({ context.step in config.beforeSteps })
        void blah(context){
            println "running before \${context.step}"
        }
        """)
        libProvider.addStep("someLibrary", "foo", """
        void call(){
            println "step: foo"
        }
        """)
        libProvider.addStep("someLibrary", "bar", """
        void call(){
            println "step: bar"
        }
        """)
        libProvider.addStep("someLibrary", "aStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary{
                    beforeSteps = [ "foo" ]
                }
                someLibrary
            }
            """,
            template: "foo(); bar()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running before foo", run)
        jenkins.assertLogNotContains("running before bar", run)        
    }

    def "Hook closure params can't invoke other steps"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        @BeforeStep({ bar() })
        void blah(context){
            println "running before \${context.step}"
        }
        """)
        libProvider.addStep("someLibrary", "foo", """
        void call(){
            println "step: foo"
        }
        """)
        libProvider.addStep("someLibrary", "bar", """
        void call(){
            println "step: bar"
        }
        """)
        libProvider.addStep("someLibrary", "aStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary
                someLibrary
            }
            """,
            template: "foo()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogNotContains("step: bar", run)
    }

    @Unroll
    def "Multiple #annotation hooks are executed"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        ${annotation} void foo(context){ println "step: foo" }
        ${annotation} void bar(context){ println "step: bar" }
        """)
        libProvider.addStep("someLibrary", "theActualStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary
                someLibrary
            }
            """,
            template: "theActualStep()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("step: foo", run)
        jenkins.assertLogContains("step: bar", run)

        where: 
        annotation << [ "@Validate", "@Init", "@BeforeStep", "@AfterStep", "@Notify", "@CleanUp"]
    }

    @Unroll
    def "#annotation adherest to conditional execution when closure param is { #shouldRun }"(){
        given: 
        def run
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("hooksLibrary", "theHooks", """
        ${annotation}({ ${shouldRun} }) void foo(context){ println "step: foo" }
        """)
        libProvider.addStep("someLibrary", "theActualStep", "void call(){ println 'the actual step' }")
        libProvider.addGlobally()

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                hooksLibrary
                someLibrary
            }
            """,
            template: "theActualStep()"
        )

        when: 
        run = job.scheduleBuild2(0).get()

        then: 
        jenkins.assertBuildStatusSuccess(run)
        if(shouldRun){
            jenkins.assertLogContains("step: foo", run)
        } else { 
            jenkins.assertLogNotContains("step: foo", run)
        }

        where: 
        annotation    | shouldRun
        "@Validate"   | true
        "@Validate"   | false
        "@Init"       | true
        "@Init"       | false
        "@BeforeStep" | true
        "@BeforeStep" | false
        "@AfterStep"  | true
        "@AfterStep"  | false
        "@Notify"     | true
        "@Notify"     | false
        "@CleanUp"    | true
        "@CleanUp"    | false
    }
}