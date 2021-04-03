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

import hudson.model.Queue
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.GlobalVariable
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Ignore
import spock.lang.Specification

class GlobalCollisionValidatorSpec extends Specification {

    // needed for testing the StepDescriptor collisions :(
    @Rule JenkinsRule jenkins = new JenkinsRule()

    GlobalCollisionValidator validator = new GlobalCollisionValidator()

    TemplateLogger logger = Mock()
    Map<String, List<TemplatePrimitive>> primitivesByName = [:]
    DummyExecutionOwner flowOwner = Mock()
    PipelineConfigurationObject config = Mock {
        getJteBlockWrapper() >> [ permissive_initialization: false ]
    }

    /* it's unnecessarily difficult to mock Owner.run() */
    class DummyExecutionOwner extends FlowExecutionOwner {

        WorkflowRun run() {
            return null
        }

        @Override
        FlowExecution get() throws IOException {
            return null
        }

        @Override
        File getRootDir() throws IOException {
            return null
        }

        @Override
        Queue.Executable getExecutable() throws IOException {
            return null
        }

        @Override
        String getUrl() throws IOException {
            return null
        }

        @Override
        boolean equals(Object o) {
            return false
        }

        @Override
        int hashCode() {
            return 0
        }

    }

    static class DummyPrimitive extends TemplatePrimitive { }

    @WithoutJenkins
    def "checkForPrimitiveCollisions: No Primitives = No Problem"() {
        when:
        validator.checkForPrimitiveCollisions(primitivesByName, config, logger)

        then:
        noExceptionThrown()
        0 * logger./.*/(*_) // no methods invoked on the logger
    }

    @WithoutJenkins
    def "checkForPrimitiveCollisions: No Collisions = No Problem"() {
        given:
        primitivesByName = [
            build: [ new DummyPrimitive(name: 'build') ],
            test: [ new DummyPrimitive(name: 'test') ]
        ]

        when:
        validator.checkForPrimitiveCollisions(primitivesByName, config, logger)

        then:
        noExceptionThrown()
        0 * logger./.*/(*_) // no methods invoked on the logger
    }

    @WithoutJenkins
    def "checkForPrimitiveCollisions: primitive collisions throws exception when permissive_initialization = false"() {
        given:
        DummyPrimitive build = new DummyPrimitive(name: 'build')
        primitivesByName = [
           build: [ build, build ]
        ]

        when:
        validator.checkForPrimitiveCollisions(primitivesByName, config, logger)

        then:
        thrown(JTEException)
    }

    @WithoutJenkins
    def "checkForPrimitiveCollisions: primitive collisions logs collision when permissive_initialization = false"() {
        given:
        DummyPrimitive build = new DummyPrimitive(name: 'build')
        primitivesByName = [
                build: [ build, build ]
        ]

        when:
        validator.checkForPrimitiveCollisions(primitivesByName, config, logger)

        then:
        3 * logger.printError(_) // once for header.. twice for each collision
        thrown(JTEException)
    }

    @WithoutJenkins
    def "checkForPrimitiveCollisions: primitive collisions logs multiple collisions when permissive_initialization = false"() {
        given:
        DummyPrimitive build = new DummyPrimitive(name: 'build')
        primitivesByName = [
                build: [ build, build, build ]
        ]

        when:
        validator.checkForPrimitiveCollisions(primitivesByName, config, logger)

        then:
        thrown(JTEException)
        4 * logger.printError(_)
    }

    @WithoutJenkins
    def "checkForPrimitiveCollisions: collisions are okay when permissive_initialization = true"() {
        given:
        DummyPrimitive build = new DummyPrimitive(name: 'build')
        primitivesByName = [
                build: [ build, build, build ]
        ]

        when:
        validator.checkForPrimitiveCollisions(primitivesByName, config, logger)

        then:
        thrown(JTEException)
    }

    @WithoutJenkins
    def "checkForGlobalVariableCollisions: No Collisions = No Logs"() {
        when:
        validator.checkForGlobalVariableCollisions(primitivesByName, flowOwner, logger)

        then:
        noExceptionThrown()
        0 * logger./.*/(*_) // no methods invoked on the logger
    }

    @WithoutJenkins
    def "checkForGlobalVariableCollisions: Template Primitive Collisions don't count as Global Variable Collisions"() {
        given:
        DummyPrimitive build = new DummyPrimitive(name: 'build')
        primitivesByName = [
                build: [ build, build ]
        ]

        when:
        validator.checkForGlobalVariableCollisions(primitivesByName, flowOwner, logger)

        then:
        noExceptionThrown()
        0 * logger./.*/(*_) // no methods invoked on the logger
    }

    static class DummyGlobalVariable extends GlobalVariable {

        @Override
        String getName() {
            return 'docker'
        }

        @Override
        Object getValue(CpsScript script) throws Exception {
            return null
        }

    }

    @WithoutJenkins
    def "checkForGlobalVariableCollisions: GlobalVariable collision is logged"() {
        given:
        primitivesByName = [
            docker: [ new DummyPrimitive(name: 'docker') ]
        ]

        List<GlobalVariable> allVars = primitivesByName.docker + [ new DummyGlobalVariable() ]
        GroovySpy(global: true, TemplatePrimitiveCollector)
        TemplatePrimitiveCollector.getGlobalVariablesByName('docker', _) >> allVars

        when:
        validator.checkForGlobalVariableCollisions(primitivesByName, flowOwner, logger)

        then:
        noExceptionThrown()
        1 * logger.printWarning(_)
    }

    @Ignore('functionality works. not sure why JenkinsRule is freezing the test')
    def "checkForJenkinsStepCollisions: No Collisions = No Logs"() {
        given:
        primitivesByName = [
            build: [new DummyPrimitive(name: 'build') ]
        ]

        when:
        validator.checkForJenkinsStepCollisions(primitivesByName, logger)

        then:
        noExceptionThrown()
        0 * logger./.*/(*_) // no methods invoked on the logger
    }

    @Ignore('Functionality works. not sure why JenkinsRule is freezing the test')
    def "checkForJenkinsStepCollisions: Collisions are logged"() {
        given:
        primitivesByName = [
            // safe to assume the 'sh' step is present
            sh: [ new DummyPrimitive(name: 'sh') ]
        ]

        when:
        validator.checkForJenkinsStepCollisions(primitivesByName, logger)

        then:
        noExceptionThrown()
        1 * logger.printWarning(_)
    }

}
