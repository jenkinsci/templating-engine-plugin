package org.boozallen.plugins.jte.init.primitives.hooks

import spock.lang.*
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition
import hudson.model.Result
import org.boozallen.plugins.jte.init.governance.libs.TestLibraryProvider

class HookSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    /**
     * for performance, use a common jenkins and library source. 
     * individual tests will reference steps defined in this library
     */ 
    def setupSpec(){
        TestLibraryProvider libProvider = new TestLibraryProvider()
        // libProvider.addStep("exampleLibrary", "callNoParam", """
        // void call(){
        //     println "step ran"
        // }
        // """)
        libProvider.addGlobally()
    }


    def "Library config resolvable inside hook closure parameter"(){
        
    }

    def "Hook context variable resolvable inside hook closure parameter"(){
        
    }

    def "Hook closure params can't invoke other steps"(){

    }
}