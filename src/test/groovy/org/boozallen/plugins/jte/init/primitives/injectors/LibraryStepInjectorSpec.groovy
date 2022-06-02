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

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.Result
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import spock.lang.Shared
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class LibraryStepInjectorSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def cleanup(){
        TestLibraryProvider.wipeAllLibrarySources()
    }

    def "Missing library throws exception"() {
        given:
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
        config: 'libraries{ doesNotExist }',
        template: 'echo "doesnt matter"'
        )
        when:
        def run = job.scheduleBuild2(0).get()
        then:
        jenkins.assertBuildStatus(Result.FAILURE, run)
        jenkins.assertLogContains("Library doesNotExist not found", run)
    }

    def "library is loaded successfully"(){
        given:
        TestLibraryProvider lib = new TestLibraryProvider()
        lib.addStep("example", "step", """
        void call(){
          println "do nothing"
        }
        """)
        lib.addGlobally()
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
        config: 'libraries{ example }',
        template: 'step()'
        )
        when:
        def run = job.scheduleBuild2(0).get()
        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    def "Libraries can be loaded across library sources in a governance tier"() {
        given:
        // add two global library sources
        [1,2].each{ i ->
            TestLibraryProvider lib = new TestLibraryProvider()
            lib.addStep("lib${i}", "step${i}", """
            void call(){
              println "do nothing"
            }
            """)
            lib.addGlobally()
        }
        WorkflowJob job = TestUtil.createAdHoc(jenkins,
        config: 'libraries{ lib1; lib2 }',
        template: 'step1(); step2()'
        )
        when:
        def run = job.scheduleBuild2(0).get()
        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    def "Libraries can be loaded across library sources in different governance tiers"() {
        given:
        // add global
        TestLibraryProvider lib = new TestLibraryProvider()
        lib.addStep("lib1", "step1", """
        void call(){
          println "do nothing"
        }
        """)
        lib.addGlobally()
        // add folder
        TestLibraryProvider lib2 = new TestLibraryProvider()
        lib2.addStep("lib2", "step2", """
        void call(){
          println "do nothing"
        }
        """)
        Folder folder = jenkins.createProject(Folder)
        lib2.addToFolder(folder)
        // create job
        WorkflowJob job = TestUtil.createAdHocInFolder(folder,
        config: 'libraries{ lib1; lib2 }',
        template: 'step1(); step2()'
        )
        when:
        def run = job.scheduleBuild2(0).get()
        then:
        jenkins.assertBuildStatusSuccess(run)
    }

    def "library on more granular governance tier gets loaded"() {
        given:
        // add global
        TestLibraryProvider lib = new TestLibraryProvider()
        lib.addStep("lib", "step", """
        void call(){
          println "global step"
        }
        """)
        lib.addGlobally()
        // add folder
        TestLibraryProvider lib2 = new TestLibraryProvider()
        lib2.addStep("lib", "step", """
        void call(){
          println "folder step"
        }
        """)
        Folder folder = jenkins.createProject(Folder)
        lib2.addToFolder(folder)
        // create job
        WorkflowJob job = TestUtil.createAdHocInFolder(folder,
                config: 'libraries{ lib }',
                template: 'step()'
        )
        when:
        def run = job.scheduleBuild2(0).get()
        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("folder step", run)
    }

    def "library on higher governance tier (last in hierarchy array) gets loaded if library override set to false"() {
        given:
        // add global
        TestLibraryProvider lib = new TestLibraryProvider()
        lib.addStep("lib", "step", """
        void call(){
          println "global step"
        }
        """)
        lib.addGlobally()
        // add folder
        TestLibraryProvider lib2 = new TestLibraryProvider()
        lib2.addStep("lib", "step", """
        void call(){
          println "folder step"
        }
        """)
        Folder folder = jenkins.createProject(Folder)
        lib2.addToFolder(folder)
        // create job
        WorkflowJob job = TestUtil.createAdHocInFolder(folder,
                config: 'jte{ reverse_library_resolution = true } ; libraries{ lib }',
                template: 'step()'
        )
        when:
        def run = job.scheduleBuild2(0).get()
        then:
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("global step", run)
    }

}
