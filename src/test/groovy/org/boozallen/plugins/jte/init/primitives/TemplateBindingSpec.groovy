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

import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Result
import hudson.model.TaskListener
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification

class TemplateBindingSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    TemplateBinding binding = new TemplateBinding(Mock(FlowExecutionOwner), false)

    /**
     * fake primitive for testing
     */
    static class TestPrimitive extends TemplatePrimitive{
        String name
        Class<? extends TemplatePrimitiveInjector> injector

        @NonCPS @Override String getName(){ return name }
        @NonCPS @Override Class<? extends TemplatePrimitiveInjector> getInjector(){ return injector }

        void throwPreLockException(){
            throw new TemplateException ("pre-lock exception")
        }

        void throwPostLockException(){
            throw new TemplateException ("post-lock exception")
        }

    }

    static class TestInjector extends TemplatePrimitiveInjector{
        static String getNamespaceKey(){ return 't' }
    }

    static class LocalKeywordInjector extends TemplatePrimitiveInjector{
        static String getNamespaceKey(){ return 'lk' }
    }

    static class LocalStepInjector extends TemplatePrimitiveInjector{
    }

    /**
     * mock Keyword primitive for test
     */
    static class LocalKeyword extends TestPrimitive{
        String value = "dummy value"
        String getValue(){
            return value
        }

        @Override
        Class<? extends TemplatePrimitiveInjector> getInjector() {
            return LocalKeywordInjector
        }
    }

    /**
     * mock StepWrapper primitive for test
     */
    class StepWrapper extends TestPrimitive{}

    TemplateBinding permissiveBinding
    def setup(){
        permissiveBinding = new TemplateBinding(Mock(FlowExecutionOwner), true)
    }

    @WithoutJenkins
    def "non-primitive variable set in binding maintains value"(){
        when:
        binding.setVariable("x", 3)

        then:
        binding.getVariable("x") == 3
    }

    @WithoutJenkins
    def "Normal variable does not get inserted into registry"(){
        when:
        binding.setVariable("x", 3)

        then:
        !("x" in binding.registry)
    }

    @WithoutJenkins
    def "template primitive inserted into registry"(){
        given:
        String name = "x"

        when:
        binding.setVariable(name, new LocalKeyword(name:name))

        then:
        name in binding.registry
    }

    @WithoutJenkins
    def "binding collision pre-lock throws pre-lock exception, if not permissive"(){
        def name = "x"
        when:
        binding.setVariable(name, new LocalKeyword(name: name))
        binding.setVariable(name, 3)

        then:
        TemplateException ex = thrown(TemplateException)
        assert ex.message == "pre-lock exception"
    }

    @WithoutJenkins
    def "permissive mode binding collision does not throws pre-lock exception"(){
        def name = "x"
        when:
        permissiveBinding.setVariable(name, new LocalKeyword(name: name))
        permissiveBinding.setVariable(name, 3)

        then:
        noExceptionThrown()
    }

    @WithoutJenkins
    def "getVariable with permissive double assignment throws exception after lock"(){
        def run = Mock(FlowExecutionOwner)
        def listener = Mock(TaskListener)
        listener.getLogger() >> Mock(PrintStream)
        run.getListener() >> listener

        GroovyMock(TemplateLogger, global: true)
        TemplateLogger.createDuringRun() >> Mock(TemplateLogger)

        when:
        permissiveBinding.setVariable("x", new LocalKeyword(name: 'x'))
        permissiveBinding.setVariable("x", new TestPrimitive(name: 'x', injector: TestInjector))
        permissiveBinding.lock(run)
        permissiveBinding.getVariable('x')

        then:
        JTEException e = thrown(JTEException)
        e.message.contains("Attempted to access an overloaded primitive: x")
    }

    @WithoutJenkins
    def "binding collision post-lock throws post-lock exception"(){
        def name = "x"
        def run = Mock(FlowExecutionOwner)
        def listener = Mock(TaskListener)
        listener.getLogger() >> Mock(PrintStream)
        run.getListener() >> listener
        when:
        binding.setVariable(name, new LocalKeyword(name: name))
        binding.lock(run)
        binding.setVariable(name, 3)

        then:
        TemplateException ex = thrown()
        assert ex.message == "post-lock exception"
    }

    @WithoutJenkins
    def "missing variable throws MissingPropertyException"(){
        when:
        binding.getVariable("doesntexist")

        then:
        thrown MissingPropertyException
    }

    @WithoutJenkins
    def "getValue overrides actual value set"(){
        when:
        binding.setVariable("x", new LocalKeyword())

        then:
        binding.getVariable("x") == "dummy value"
    }

    @WithoutJenkins
    def "primitive with no getValue returns same object set"(){
        def name = "x"
        setup:
        TestPrimitive test = new TestPrimitive(name: name, injector: TestInjector)

        when:
        binding.setVariable(name, test)

        then:
        binding.getVariable(name) == test
    }

    @WithoutJenkins
    def "can't overwrite library config variable"(){
        when:
        binding.setVariable(StepWrapperFactory.CONFIG_VAR, "test")

        then:
        thrown Exception
    }

    @WithoutJenkins
    def "hasStep returns true when variable exists and is a StepWrapper"(){
        def name = "test_step"
        setup:
        GroovySpy(StepWrapperFactory, global:true)
        StepWrapperFactory.getPrimitiveClass() >> { return StepWrapper }

        when:
        StepWrapper s = new StepWrapper(name: name, injector: LocalStepInjector)
        binding.setVariable("test_step", s)

        then:
        binding.hasStep("test_step")
    }

    @WithoutJenkins
    def "hasStep returns false when variable exists but is not a StepWrapper"(){
        setup:
        binding.setVariable("test_step", 1)

        expect:
        !binding.hasStep("test_step")
    }

    @WithoutJenkins
    def "hasStep returns false when variable does not exist"(){
        expect:
        !binding.hasStep("test_step")
    }

    @WithoutJenkins
    def "getStep returns step when variable exists and is StepWrapper"(){
        def name = "test_step"
        setup:
        GroovySpy(StepWrapperFactory, global:true)
        StepWrapperFactory.getPrimitiveClass() >> { return StepWrapper }
        StepWrapper step = new StepWrapper(name: name, injector: LocalStepInjector)

        when:
        binding.setVariable(name, step)

        then:
        binding.getStep(name) == step
    }

    @WithoutJenkins
    def "getStep throws exception when variable exists but is not StepWrapper"(){
        setup:
        binding.setVariable("test_step", 1)

        when:
        binding.getStep("test_step")

        then:
        TemplateException ex = thrown()
        ex.message == "No step test_step has been loaded"
    }

    @WithoutJenkins
    def "getStep throws exception when variable does not exist"(){
        when:
        binding.getStep("test_step")

        then:
        TemplateException ex = thrown()
        ex.message == "No step test_step has been loaded"
    }

    def "validate TemplateBinding sets 'steps' var"(){
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                template: "node{ sh 'echo hello' }"
        )

        expect:
        jenkins.assertLogContains("hello", jenkins.buildAndAssertSuccess(job))
    }

    def "permissive mode binding collision with ReservedVariable (stageContext) pre-lock throws pre-lock exception"(){
        given:
        String template = """
broadway
"""
        String config = """
jte{
  permissive_initialization = true
}

stages{
  broadway{
    temp_meth1
  }
}

keywords{
  stageContext = "x"
}

template_methods{
  temp_meth1
}
"""

        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                template: template,
                config: config
        )

        expect:
        jenkins.assertLogContains("is reserved for steps to access their stage context", jenkins.buildAndAssertStatus(Result.FAILURE, job))
    }

}
