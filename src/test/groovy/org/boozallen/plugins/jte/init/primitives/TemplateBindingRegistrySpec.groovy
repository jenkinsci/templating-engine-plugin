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
import hudson.model.TaskListener
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class TemplateBindingRegistrySpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    /**
     * for performance, use a common jenkins and library source.
     * individual tests will reference steps defined in this library
     */
    @SuppressWarnings('MethodSize')
    def setupSpec(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep("exampleLibrary", "callNoParam", """
        void call(){
            println "step ran"
        }
        """)
        libProvider.addStep("exampleLibrary", "callOneParam", """
        void call(x){
            println "x=\${x}"
        }
        """)
        libProvider.addStep("exampleLibrary", "callTwoParam", """
        void call(x, y){
            println "x=\${x}"
            println "y=\${y}"
        }
        """)
        libProvider.addStep("exampleLibrary", "someStep", """
        void someMethod(){
            println "step ran"
        }
        void someMethod(x){
            println "x=\${x}"
        }
        void someMethod(x,y){
            println "x=\${x}"
            println "y=\${y}"
        }
        """)
        libProvider.addStep("exampleLibrary", "testConfig", """
        void call(){
            println "x=\${config.x}"
        }
        """)
        libProvider.addStep("exampleLibrary", "usePipelineSteps", """
        void call(){
            node{
                sh "echo canyouhearmenow"
            }
        }
        """)
        libProvider.addStep("exampleLibrary", "returnsSomething", """
        void call(){
            return "foo"
        }
        """)
        libProvider.addStep("hasHooks", "hookStep", """
        @BeforeStep
        void before(context){
            println "BeforeStep Hook"
        }
        @AfterStep
        void after(context){
            println "AfterStep Hook"
        }
        @Notify
        void notify(context){
            println "Notify Hook"
        }
        """)
        libProvider.addStep("hasHooks", "theStep", """
        void call(){
            println "the actual step"
        }
        """)
        libProvider.addStep("libA", "stepA", """
        void call(){
            println "step: A"
        }
        """)
        libProvider.addStep("libB", "stepB", """
        void call(){
            stepA()
        }
        """)
        libProvider.addStep("libB", "stepBNamespaced", """
        void call(){
            jte.libraries.libA.stepA()
        }
        """)

        libProvider.addResource("resourcesA", "myResource.txt", "my resource from resourcesA")
        libProvider.addStep("resourcesA", "fetchResource", """
        void call(){
          println resource("myResource.txt")
        }
        """)
        libProvider.addResource("resourcesA", "myOtherResource.sh", "echo hi")
        libProvider.addResource("resourcesA", "nested/somethingElse.txt", "hello, world")
        libProvider.addStep("resourcesA", "fetchNestedResource", """
        void call(){
          println resource("nested/somethingElse.txt")
        }
        """)
        libProvider.addStep("resourcesB", "fetchCrossLibrary", """
        void call(){
          println resource("nested/somethingElse.txt")
        }
        """)
        libProvider.addStep("resourcesB", "doesNotExist", """
        void call(){
          println resource("nope.txt")
        }
        """)
        libProvider.addStep("resourcesB", "absolutePath", """
        void call(){
          println resource("/nope.txt")
        }
        """)
        libProvider.addGlobally()
    }

    def "steps invocable via call shorthand with no params"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "jte.libraries.exampleLibrary.callNoParam()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("callNoParam from the exampleLibrary Library", run)
        jenkins.assertLogContains("step ran", run)
        jenkins.assertLogContains("[JTE][Step - exampleLibrary/callNoParam.call()]", run)
    }

    def "steps invocable via call shorthand with one param"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc([ config: "libraries{ exampleLibrary }",
            template: 'jte.libraries.exampleLibrary.callOneParam("foo")'], jenkins
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("x=foo", run)
    }

    def "steps invocable via call shorthand with more than one param"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: 'jte.libraries.exampleLibrary.callTwoParam("foo","bar")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("x=foo", run)
        jenkins.assertLogContains("y=bar", run)
    }

    def "steps can invoke non-call methods with no params"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "jte.libraries.exampleLibrary.someStep.someMethod()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("step ran", run)
    }

    def "steps can invoke non-call methods with 1 param"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: 'jte.libraries.exampleLibrary.someStep.someMethod("foo")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("x=foo", run)
    }

    def "steps can invoke non-call methods with more than 1 param"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: 'jte.libraries.exampleLibrary.someStep.someMethod("foo", "bar")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("x=foo", run)
        jenkins.assertLogContains("y=bar", run)
    }

    def "steps can access configuration via config variable"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                exampleLibrary{
                    x = "foo"
                }
            }
            """,
            template: 'jte.libraries.exampleLibrary.testConfig()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("x=foo", run)
    }

    def "steps can invoke pipeline steps directly"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "jte.libraries.exampleLibrary.usePipelineSteps()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("canyouhearmenow", run)
    }

    def "return step return result through StepWrapper"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: """
            x = jte.libraries.exampleLibrary.returnsSomething()
            println "x=\${x}"
            """
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("x=foo", run)
    }

    def "step method not found throws TemplateException"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: "jte.libraries.exampleLibrary.callNoParam.nonExistent()"
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Step callNoParam from the library exampleLibrary does not have the method nonExistent()", run)
    }

    def "step override during initialization throws exception"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                exampleLibrary
            }
            keywords{
                callNoParam = "oops"
            }
            """,
            template: 'println "doesnt matter"'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
    }

    def "step override during initialization success with permissive_initialization"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            jte{
                permissive_initialization = true
            }
            libraries{
                exampleLibrary
            }
            keywords{
                callNoParam = "oops"
            }
            """, template: 'println "doesnt matter"'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.SUCCESS, run)
    }

    def "step override post initialization throws exception"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: "libraries{ exampleLibrary }",
            template: 'jte.libraries.exampleLibrary.callNoParam = "oops"'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
    }

    def "Step can invoke another step"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: """
            libraries{
                libA
                libB
            }
            """,
            template: 'stepB()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("step: A", run)
    }

    def "Namespaced Step can invoke another step"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            libraries{
                libA
                libB
            }
            """, template: 'jte.libraries.libB.stepB()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("step: A", run)
    }

    def "Namespaced Step can invoke another Namespaced step"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            libraries{
                libA
                libB
            }
            """, template: 'jte.libraries.libB.stepBNamespaced()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("step: A", run)
    }

    @Ignore
    def "Namespaced Default Step invoked"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            libraries{
                libA
                libB
            }

            steps{
              stepD{
                 image = "alpine:latest"
                 command = "echo 'step: D'"
              }
            }
            """, template: 'jte.steps.stepD()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("step: D", run)
    }

    @Ignore
    def "Incorrectly Namespaced Library Step call throws MissingMethodException"(){
        given:
        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins, config: """
            libraries{
                libA
                libB
            }

            steps{
              stepD{
                 command = "echo 'stepB'"
              }
            }
            """, template: 'jte.libraries.libB.stepD()'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("MissingMethodException", run)
    }

    @WithoutJenkins
    @Unroll
    def "overriding autowired variable #var in binding with permissive: #permissive throws exception"(){
        FlowExecutionOwner run = Mock(FlowExecutionOwner)
        TaskListener listener = Mock(TaskListener)
        listener.getLogger() >> Mock(PrintStream)
        run.getListener() >> listener

        given:
        TemplateBinding binding = new TemplateBinding(run, permissive)
        when:
        binding.setVariable(var, "_")
        then:
        thrown(Exception)
        where:
        var << [ "config", "stageContext", "hookContext", "resource", "config", "stageContext", "hookContext", "resource" ]
        permissive << [ false, false, false, false, true, true, true, true]
    }

}
