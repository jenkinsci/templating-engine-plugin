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

package org.boozallen.plugins.jte.init.governance.libs

import hudson.model.TaskListener
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.extensions.GitSCMExtension
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.TestFlowExecutionOwner
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class ScmLibraryProviderSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    @Rule GitSampleRepoRule repo = new GitSampleRepoRule()

    TestFlowExecutionOwner owner
    PrintStream logger = Mock()

    def setup(){
        WorkflowJob job = GroovyMock()
        job.asBoolean() >> true
        WorkflowRun run = GroovyMock()
        run.getParent() >> job
        TaskListener listener = Mock()
        listener.getLogger() >> logger
        owner = Mock() 
        owner.getListener() >> listener
        owner.run() >> run
    }
    
    def "hasLibrary returns true when library exists"(){
        given: 
        ScmLibraryProvider p = new ScmLibraryProvider() 
        String libraryName = "someLibrary"
        repo.init() 
        repo.write("${libraryName}/someStep.groovy", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.hasLibrary(owner, libraryName)
    }

    def "hasLibrary returns false when library does not exist"(){
        given: 
        ScmLibraryProvider p = new ScmLibraryProvider() 
        String libraryName = "someLibrary"
        repo.init() 
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        !p.hasLibrary(owner, libraryName)
    }

    static class StepWrapper{
        String name

        StepWrapper(String name){
            this.name = name
        }

    }

    def "loadLibrary puts step into binding"(){
        given: 
        ScmLibraryProvider p = new ScmLibraryProvider() 
        String libraryName = "someLibrary"
        repo.init() 
        repo.write("${libraryName}/someStep.groovy", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        GroovySpy(StepWrapperFactory, global:true)
        new StepWrapperFactory(_) >> Mock(StepWrapperFactory){
            createFromFile(*_) >> { args ->
                String name = args[0].getName() - ".groovy"
                return new StepWrapper(name)
            }
        }

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        def binding = new Binding()

        when: 
        p.loadLibrary(owner, binding, libraryName, [:])

        then:
        binding.hasVariable("someStep")
    }

    def "loadLibrary logs library being loaded"(){
        given: 
        ScmLibraryProvider p = new ScmLibraryProvider() 
        String libraryName = "someLibrary"
        repo.init() 
        repo.write("${libraryName}/someStep.groovy", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        GroovySpy(StepWrapperFactory, global:true)
        new StepWrapperFactory(_) >> Mock(StepWrapperFactory){
            createFromFile(*_) >> { args ->
                String name = args[0].getName() - ".groovy"
                return new StepWrapper(name)
            }
        }

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw
        
        def binding = new Binding()

        when: 
        p.loadLibrary(owner, binding, libraryName, [:])

        then:
        1 * logger.println("[JTE] Loading Library someLibrary")
    }

    def "loadLibrary logs if library does not have library configuration file"(){
        given: 
        ScmLibraryProvider p = new ScmLibraryProvider() 
        String libraryName = "someLibrary"
        repo.init() 
        repo.write("${libraryName}/someStep.groovy", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        GroovySpy(StepWrapperFactory, global:true)
        new StepWrapperFactory(_) >>  Mock(StepWrapperFactory){
            createFromFile(*_) >> { args ->
                String name = args[0].getName() - ".groovy"
                return new StepWrapper(name)
            }
        }

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw
        
        def binding = new Binding()

        when: 
        p.loadLibrary(owner, binding, libraryName, [:])

        then:
        1 * logger.println("[JTE] Library someLibrary does not have a configuration file.")
    }

    def "loadLibrary invokes library configuration validation if lib config file present"(){
        given: 
        ScmLibraryProvider p = new ScmLibraryProvider()
        
        String libraryName = "someLibrary"
        repo.init() 
        repo.write("${libraryName}/someStep.groovy", "void call(){ println 'the step' }")
        repo.write("${libraryName}/library_config.groovy", """
        fields{
            required{
                x = String
            }
        }
        """)
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        GroovySpy(EnvActionImpl, global: true)
        EnvActionImpl.forRun(_) >> Mock(EnvActionImpl)

        GroovySpy(StepWrapperFactory, global:true)
        StepWrapperFactory.getPrimitiveClass() >> StepWrapper

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw
        
        def binding = new Binding()
        def result

        when:
        result = p.loadLibrary(owner, binding, libraryName, [:])

        then:
        result == [ "${libraryName}:", " - Missing required field 'x'" ]
    }

    @Unroll
    @WithoutJenkins
    def "when baseDir='#baseDir' then prefixBaseDir('#arg') is #expected "(){
        setup:
        ScmLibraryProvider libSource = new ScmLibraryProvider()
        libSource.setBaseDir(baseDir)

        expect:
        libSource.prefixBaseDir(arg) == expected

        where:
        baseDir    | arg    || expected
        " someDir" | "lib"  || "someDir/lib"
        "someDir " | "lib"  || "someDir/lib"
        "someDir"  | " lib" || "someDir/lib"
        "someDir"  | "lib " || "someDir/lib"
        "someDir"  | null   || "someDir"
        "someDir"  | ""     || "someDir"
        null       | "lib"  || "lib"
        ""         | "lib"  || "lib"
        null       | null   || ""
    }

    GitSCM createSCM(_repo){
        return new GitSCM(
            GitSCM.createRepoList(_repo.toString(), null),
            Collections.singletonList(new BranchSpec("*/master")),
            false,
            Collections.<SubmoduleConfig>emptyList(),
            null,
            null,
            Collections.<GitSCMExtension>emptyList()
        )
    }    
}