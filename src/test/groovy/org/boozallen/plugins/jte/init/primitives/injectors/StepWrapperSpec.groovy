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
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

class StepWrapperSpec extends Specification {

    // shared to make the test suite faster
    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    TestLibraryProvider libProvider = new TestLibraryProvider()

    // add the new library source for each test
    def setup() {
        libProvider.addGlobally()
    }

    // after each test, remove the library source
    // for a fresh start.
    def cleanup(){
        TestLibraryProvider.removeLibrarySources()
    }

    def "Library class can be imported and used in a pipeline template"() {
        given:
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{
          void doThing(steps){ steps.echo "doing a thing" }
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ utility }',
            template: '''
            import jte.Utility

            Utility u = new Utility()
            u.doThing(steps)
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('doing a thing', run)
    }

    def "Library class can be imported and used from same library"() {
        given:
        libProvider.addSrc('utility', 'src/boozallen/Utility.groovy', '''
        package boozallen
        class Utility implements Serializable{
          void doThing(steps){ steps.echo "doing a thing" }
        }
        ''')
        libProvider.addStep('utility', 'useClass', '''
        import boozallen.Utility
        void call(){
          Utility u = new Utility()
          u.doThing(steps)
        }
        ''')

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ utility }',
                template: 'useClass()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('doing a thing', run)
    }

    def "Library class from A can be used in B when A is loaded first"() {
        given:
        libProvider.addSrc('utility', 'src/boozallen/Utility.groovy', '''
        package boozallen
        class Utility implements Serializable{
          void doThing(steps){ steps.echo "doing a thing" }
        }
        ''')
        libProvider.addStep('utility', 'useClass', '''
        import boozallen.Utility
        void call(){
          Utility u = new Utility()
          u.doThing(steps)
        }
        ''')
        libProvider.addStep('otherLibrary', 'useClassB', '''
        import boozallen.Utility
        void call(){
          Utility u = new Utility()
          u.doThing(steps)
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: '''
                libraries{
                  utility
                  otherLibrary
                }''',
                template: 'useClassB()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('doing a thing', run)
    }

    def "Library class from A can be used in B when B is loaded first"() {
        given:
        libProvider.addSrc('utility', 'src/boozallen/Utility.groovy', '''
        package boozallen
        class Utility implements Serializable{
          void doThing(steps){ steps.echo "doing a thing" }
        }
        ''')
        libProvider.addStep('utility', 'useClass', '''
        import boozallen.Utility
        void call(){
          Utility u = new Utility()
          u.doThing(steps)
        }
        ''')
        libProvider.addStep('otherLibrary', 'useClassB', '''
        import boozallen.Utility
        void call(){
          Utility u = new Utility()
          u.doThing(steps)
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: '''
                libraries{
                  otherLibrary
                  utility
                }''',
                template: 'useClassB()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('doing a thing', run)
    }

    def "steps invocable via call shorthand with no params"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            println "step ran"
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('step ran', run)
    }

    def "step logs invocation"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            println "step ran"
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('[JTE][Step - exampleLibrary/step.call()]', run)
    }

    def "steps invocable via call shorthand with one param"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', """
        void call(x){
            println "x=\${x}"
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step("foo")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
    }

    def "steps invocable via call shorthand with more than one param"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', """
        void call(x, y){
            println "x=\${x}"
            println "y=\${y}"
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step("foo","bar")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
        jenkins.assertLogContains('y=bar', run)
    }

    def "steps can invoke non-call methods with no params"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', """
        void someMethod(){
            println "step ran"
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step.someMethod()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('step ran', run)
    }

    def "steps can invoke non-call methods with 1 param"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', """
        void someMethod(x){
            println "x=\${x}"
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step.someMethod("foo")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
    }

    def "steps can invoke non-call methods with more than 1 param"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', """
        void someMethod(x,y){
            println "x=\${x}"
            println "y=\${y}"
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step.someMethod("foo", "bar")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
        jenkins.assertLogContains('y=bar', run)
    }

    def "steps can access configuration via config variable"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', """
        void call(){
            println "x=\${config.x}"
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                exampleLibrary{
                    x = "foo"
                }
            }
            ''',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
    }

    def "steps can invoke pipeline steps directly"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            node{
                sh "echo canyouhearmenow"
            }
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('canyouhearmenow', run)
    }

    def "return step return result through StepWrapper"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            return "foo"
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: """
            x = step()
            println "x=\${x}"
            """
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('x=foo', run)
    }

    def "step method not found throws TemplateException"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            println "step ran"
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step.nonExistent()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains('Step step from the library exampleLibrary does not have the method nonExistent()', run)
    }

    def "step override during initialization throws exception"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            println "step ran"
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                exampleLibrary
            }
            keywords{
                step = "oops"
            }
            ''',
            template: 'println "doesnt matter"'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
    }

    def "step override post initialization throws exception"() {
        given:
        libProvider.addStep('exampleLibrary', 'step', '''
        void call(){
            println "step ran"
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ exampleLibrary }',
            template: 'step = "oops"'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
    }

    def "Step can invoke another step"() {
        given:
        libProvider.addStep('libA', 'stepA', '''
        void call(){
            println "step: A"
        }
        ''')
        libProvider.addStep('libB', 'stepB', '''
        void call(){
            stepA()
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                libA
                libB
            }
            ''',
            template: 'stepB()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('step: A', run)
    }

    def "library resource can be fetched within a step"() {
        given:
        libProvider.addResource('resourcesA', 'myResource.txt', 'my resource from resourcesA')
        libProvider.addStep('resourcesA', 'step', '''
        void call(){
          println resource("myResource.txt")
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                resourcesA
            }
            ''',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('my resource from resourcesA', run)
    }

    def "library resource method can't be called from outside a step"() {
        given:
        libProvider.addResource('resourcesA', 'myResource.txt', 'my resource from resourcesA')
        libProvider.addStep('resourcesA', 'step', '''
        void call(){
          println resource("myResource.txt")
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                resourcesA
            }
            ''',
            template: 'resource("myResource.txt)'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogNotContains('my resource from resourcesA', run)
    }

    def "nested library resource can be fetched within a step"() {
        given:
        libProvider.addResource('resourcesA', 'nested/somethingElse.txt', 'hello, world')
        libProvider.addStep('resourcesA', 'step', '''
        void call(){
          println resource("nested/somethingElse.txt")
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                resourcesA
            }
            ''',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains('hello, world', run)
    }

    def "step can only retrieve resource from own library"() {
        given:
        libProvider.addResource('resourcesA', 'nested/somethingElse.txt', 'hello, world')
        libProvider.addStep('resourcesA', 'stepA', '''
        void call(){
          println resource("nested/somethingElse.txt")
        }
        ''')
        libProvider.addStep('resourcesB', 'stepB', '''
        void call(){
          println resource("nested/somethingElse.txt")
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                resourcesA
                resourcesB
            }
            ''',
            template: 'stepA(); stepB()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains('hello, world', run)
        jenkins.assertLogContains("JTE: library step requested a resource 'nested/somethingElse.txt' that does not exist", run)
    }

    def "step fetching non-existent resource throws exception"() {
        given:
        libProvider.addStep('resourcesB', 'step', '''
        void call(){
          println resource("nope.txt")
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                resourcesB
            }
            ''',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("JTE: library step requested a resource 'nope.txt' that does not exist", run)
    }

    def "step fetching resource with absolute path throws exception"() {
        given:
        libProvider.addStep('resourcesB', 'step', '''
        void call(){
          println resource("/nope.txt")
        }
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: '''
            libraries{
                resourcesB
            }
            ''',
            template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains('JTE: library step requested a resource that is not a relative path.', run)
    }

    def "getParentChain returns the correct path"() {
        given:
        libProvider.addStep('resourcesB', 'step', '''
        void call(){
          println resource("/nope.txt")
        }
        ''')
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ resourcesB }',
            template: "println 'doesnt matter'"
        )
        WorkflowRun run = job.scheduleBuild2(0).waitForStart()
        jenkins.waitForCompletion(run)
        TemplatePrimitiveCollector c = run.getAction(TemplatePrimitiveCollector)
        StepWrapper step = c.findAll { primitive ->
            primitive.getName() == 'step'
        }.first()

        expect:
        jenkins.assertBuildStatusSuccess(run)
        step.getParentChain() == 'jte.libraries.resourcesB.step'
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/279")
    def "library Class instanceof works in same library step"(){
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{}
        ''')
        libProvider.addStep("utility", "step", """
        import jte.Utility
        void call(){
            Utility u = new Utility()
            assert u instanceof Utility
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ utility }',
                template: 'step()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/279")
    def "library Class instanceof works in same library different step"(){
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{}
        ''')
        libProvider.addStep("utility", "createUtility", """
        import jte.Utility
        def call(){
            return new Utility()
        }
        """)
        libProvider.addStep("utility", "checkUtility", """
        import jte.Utility
        void call(def u){
          println Utility.getClassLoader()
          println u.class.getClassLoader()
          assert u instanceof Utility
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ utility }',
            template: '''
            def u = createUtility()
            checkUtility(u)
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/279")
    def "library Class instanceof works in different library step"(){
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{}
        ''')
        libProvider.addStep("libA", "createUtility", """
        import jte.Utility
        def call(){
            return new Utility()
        }
        """)
        libProvider.addStep("libB", "checkUtility", """
        import jte.Utility
        void call(def u){
          assert u instanceof Utility
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ utility; libA; libB }',
                template: '''
            def u = createUtility()
            checkUtility(u)
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/279")
    def "library Class instanceof works in template"(){
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{}
        ''')
        libProvider.addStep("utility", "createUtility", """
        import jte.Utility
        def call(){
            return new Utility()
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ utility }',
                template: '''
            import jte.Utility
            def u = createUtility()
            assert u instanceof Utility
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/279")
    def "library Class instanceof works in template when created in template"(){
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{}
        ''')
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ utility }',
            template: '''
            import jte.Utility
            def u = new Utility()
            assert u instanceof Utility
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/279")
    def "Step method parameters can be typed to library classes"(){
        libProvider.addSrc('utility', 'src/jte/Utility.groovy', '''
        package jte
        class Utility implements Serializable{}
        ''')
        libProvider.addStep("utility", "checkUtility", """
        import jte.Utility
        void call(Utility u){
          assert u instanceof Utility
        }
        """)
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ utility }',
            template: '''
            import jte.Utility
            def u = new Utility()
            checkUtility(u)
            '''
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

}
