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

import org.boozallen.plugins.jte.init.PipelineDecorator
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.util.AggregateException
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification

class LibraryStepInjectorSpec extends Specification{

    class JobChild {
        WorkflowJob getParent(){ return null }
    }

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    CpsScript script = Mock()
    PrintStream logger = Mock()

    WorkflowJob job = GroovyMock()
    LibraryStepInjector injector = new LibraryStepInjector()
    FlowExecutionOwner flowExecutionOwner = GroovyMock{
        run() >> GroovyMock(JobChild){
            getParent() >> job
        }
    }

    TemplateBinding templateBinding = Mock()
    LinkedHashMap config = [
            jte: [:],
            libraries: [:]
    ]

    PipelineConfigurationObject pipelineConfigurationObject = pipelineConfigurationObject = Mock{
        getConfig() >> config

        getJteBlockWrapper() >> { return config.jte as PipelineDecorator.JteBlockWrapper }
    }

    class MockLibraryProvider extends LibraryProvider{

        @Override
        Boolean hasLibrary(FlowExecutionOwner flowOwner, String libraryName) {
            return false
        }

        @Override
        String getLibrarySchema(FlowExecutionOwner flowOwner, String libraryName) {
            return null
        }

        @Override
        void logLibraryLoading(FlowExecutionOwner flowOwner, String libName){}

        @Override
        void loadLibraryClasses(FlowExecutionOwner flowOwner, String libName){}

        @Override
        void loadLibrarySteps(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig) {}

    }

    @WithoutJenkins
    def "when library source has library, loadLibrary is called"(){
        setup:
        String libraryName = "libA"
        config.libraries["libA"] = [:]

        MockLibraryProvider p1 = Mock{
            hasLibrary(flowExecutionOwner, libraryName) >> true
        }

        LibrarySource s1 = Mock{
            getLibraryProvider() >> p1
        }

        GovernanceTier t1 = GroovyMock(global:true){
            getLibrarySources() >> [ s1 ]
        }

        GovernanceTier.getHierarchy(_) >> [ t1 ]

        when:
        injector.injectPrimitives(flowExecutionOwner, pipelineConfigurationObject, templateBinding)

        then:
        1 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, libraryName, _)
    }

    @WithoutJenkins
    def "Libraries can be loaded across library sources in a governance tier"(){
        setup:
        config.libraries["libA"] = [:]
        config.libraries["libB"] = [:]

        MockLibraryProvider p1 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
        }
        MockLibraryProvider p2 = Mock{
            hasLibrary(flowExecutionOwner, "libB") >> true
        }

        LibrarySource s1 = Mock{
            getLibraryProvider() >> p1
        }
        LibrarySource s2 = Mock{
            getLibraryProvider() >> p2
        }

        GovernanceTier t1 = GroovyMock(global:true){
            getLibrarySources() >> [ s1, s2 ]
        }

        GovernanceTier.getHierarchy(_) >> [ t1 ]

        when:
        injector.injectPrimitives(flowExecutionOwner, pipelineConfigurationObject, templateBinding)

        then:
        1 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
        0 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libB", _)
        1 * p2.loadLibrarySteps(flowExecutionOwner, templateBinding, "libB", _)
        0 * p2.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
    }

    @WithoutJenkins
    def "Libraries can be loaded across library sources in different governance tiers"(){
        setup:
        config.libraries["libA"] = [:]
        config.libraries["libB"] = [:]

        MockLibraryProvider p1 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
        }
        MockLibraryProvider p2 = Mock{
            hasLibrary(flowExecutionOwner, "libB") >> true
        }

        LibrarySource s1 = Mock{
            getLibraryProvider() >> p1
        }
        LibrarySource s2 = Mock{
            getLibraryProvider() >> p2
        }

        GovernanceTier tier1 = Mock{
            getLibrarySources() >> [ s1, s2 ]
        }

        GovernanceTier tier2 = GroovyMock(global:true){
            getLibrarySources() >> [ s1, s2 ]
        }

        GovernanceTier.getHierarchy(_) >> [ tier1, tier2 ]

        when:
        injector.injectPrimitives(flowExecutionOwner, pipelineConfigurationObject, templateBinding)

        then:
        1 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
        0 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libB", _)
        0 * p2.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
        1 * p2.loadLibrarySteps(flowExecutionOwner, templateBinding, "libB", _)
    }

    @WithoutJenkins
    def "library on more granular governance tier gets loaded"(){
        setup:
        config.libraries["libA"] = [:]

        MockLibraryProvider p1 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
        }
        MockLibraryProvider p2 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
        }

        LibrarySource s1 = Mock{
            getLibraryProvider() >> p1
        }
        LibrarySource s2 = Mock{
            getLibraryProvider() >> p2
        }

        GovernanceTier tier1 = Mock{
            getLibrarySources() >> [ s1 ]
        }

        GovernanceTier tier2 = GroovyMock(global:true){
            getLibrarySources() >> [ s2 ]
        }

        GovernanceTier.getHierarchy(_) >> [ tier1, tier2 ]

        when:
        injector.injectPrimitives(flowExecutionOwner, pipelineConfigurationObject, templateBinding)

        then:
        1 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
        0 * p2.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
    }

    @WithoutJenkins
    def "library on higher governance tier (last in hierarchy array) gets loaded if library override set to false"(){
        setup:
        config.jte['reverse_library_resolution'] = true
        config.libraries["libA"] = [:]

        MockLibraryProvider p1 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
        }
        MockLibraryProvider p2 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
        }

        LibrarySource s1 = Mock{
            getLibraryProvider() >> p1
        }
        LibrarySource s2 = Mock{
            getLibraryProvider() >> p2
        }

        GovernanceTier t1 = Mock{
            getLibrarySources() >> [ s1 ]
        }

        GovernanceTier t2 = GroovyMock(global:true){
            getLibrarySources() >> [ s2 ]
        }

        GovernanceTier.getHierarchy(_) >> [ t1, t2 ]

        when:
        injector.injectPrimitives(flowExecutionOwner, pipelineConfigurationObject, templateBinding)

        then:
        0 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
        1 * p2.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", _)
    }

    @WithoutJenkins
    def "library loader correctly passes step config"(){
        setup:
        config.libraries = [
                libA: [
                        fieldA: "A"
                ],
                libB: [
                        fieldB: "B"
                ]
        ]

        MockLibraryProvider p1 = Mock{
            hasLibrary(flowExecutionOwner, "libA") >> true
            hasLibrary(flowExecutionOwner, "libB") >> true
        }

        LibrarySource s1 = Mock{
            getLibraryProvider() >> p1
        }

        GovernanceTier t1 = GroovyMock(global:true){
            getLibrarySources() >> [ s1 ]
        }

        GovernanceTier.getHierarchy(_) >> [ t1 ]

        when:
        injector.injectPrimitives(flowExecutionOwner, pipelineConfigurationObject, templateBinding)

        then:
        1 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libA", [fieldA: "A"])
        1 * p1.loadLibrarySteps(flowExecutionOwner, templateBinding, "libB", [fieldB: "B"])
    }

    @WithoutJenkins
    def "Missing library throws exception"(){
        // now, when a library isn't found, we push a message onto the `libConfigErrors` array
        // and throw the exception later after validating all the libraries.
        // so this test represents making sure that an exception is thrown if a library does not exist.
        setup:
        config.libraries = [
                libA: [
                        fieldA: "A"
                ],
                libB: [
                        fieldB: "B"
                ]
        ]

        MockLibraryProvider p = Mock{
            1 * hasLibrary(flowExecutionOwner, "libA") >> true
            1 * hasLibrary(flowExecutionOwner, "libB") >> false
        }

        LibrarySource s = Mock{
            getLibraryProvider() >> p
        }

        GovernanceTier tier = GroovyMock(global:true){
            getLibrarySources() >> [ s ]
        }

        GovernanceTier.getHierarchy(_) >> [ tier ]

        when:
        injector.validateConfiguration(flowExecutionOwner, pipelineConfigurationObject)

        then:
        thrown(AggregateException)
    }

}
