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

import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

class TemplatePrimitiveNamespaceSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    @Issue("https://github.com/jenkinsci/templating-engine-plugin/issues/255")
    def "Namespaced Step call passes named arguments as map"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('lib1', 'someStep', '''
        void call(def opts = [:]){
            println opts.toString()
            assert opts.foo == "bar"
        }
        ''')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ lib1 }',
            template: 'jte.libraries.lib1.someStep(foo: "bar")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    def "Namespaced Step call passes single individual parameters correctly"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('lib2', 'someStep', '''
        void call(String foo){
            assert foo == "bar"
        }
        ''')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
            config: 'libraries{ lib2 }',
            template: 'jte.libraries.lib2.someStep("bar")'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    def "Namespaced Step call passes multiple individual parameters correctly"(){
        given:
        TestLibraryProvider libProvider = new TestLibraryProvider()
        libProvider.addStep('lib3', 'someStep', '''
        void call(String foo, def x){
            assert foo == "bar"
            assert x == 11
        }
        ''')
        libProvider.addGlobally()

        def run
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
                config: 'libraries{ lib3 }',
                template: 'jte.libraries.lib3.someStep("bar", 11)'
        )

        when:
        run = job.scheduleBuild2(0).get()

        then:
        jenkins.assertBuildStatusSuccess(run)
    }

}
