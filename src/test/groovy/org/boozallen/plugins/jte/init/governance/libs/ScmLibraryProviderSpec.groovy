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

import hudson.FilePath
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
        WorkflowJob job = jenkins.createProject(WorkflowJob)
        job.asBoolean() >> true
        WorkflowRun run = GroovyMock()
        run.getParent() >> job
        TaskListener listener = Mock()
        listener.getLogger() >> logger
        owner = Mock()
        owner.getListener() >> listener
        owner.run() >> run
    }

    def "hasLibrary returns true when library exists, has steps directory with any *.groovy files"(){
        given:
        ScmLibraryProvider p = new ScmLibraryProvider()
        String libraryName = "someLibrary"
        repo.init()
        repo.write("${libraryName}/steps/someStep.groovy", "void call(){ println 'the step' }")
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

    def "hasLibrary returns true when library exists, has steps directory with any *.groovy files, multiple directories down"(){
        given:
        ScmLibraryProvider p = new ScmLibraryProvider()
        String libraryName = "someLibrary"
        repo.init()
        repo.write("${libraryName}/steps/dir1/dir2/someStep.groovy", "void call(){ println 'the step' }")
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

    def "hasLibrary returns false when library exists but has no steps directory"(){
        given:
        ScmLibraryProvider p = new ScmLibraryProvider()
        String libraryName = "someLibrary"
        repo.init()
        repo.write("${libraryName}/resources/someStep.groovy", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw
        1 * logger.println{ msg -> msg.contains("Library ${libraryName} exists but does not have steps present".toString()) }

        expect:
        p.hasLibrary(owner, libraryName) == false
    }

    def "hasLibrary returns false when library step directory exists but has no steps *.groovy"(){
        given:
        ScmLibraryProvider p = new ScmLibraryProvider()
        String libraryName = "someLibrary"
        repo.init()
        repo.write("${libraryName}/steps/someStep.sh", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw
        1 * logger.println{ msg -> msg.contains("Library ${libraryName} exists but does not have steps present".toString()) }

        expect:
        p.hasLibrary(owner, libraryName) == false
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
        repo.write("${libraryName}/steps/someStep.groovy", "void call(){ println 'the step' }")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)

        WorkflowJob job = jenkins.createProject(WorkflowJob)
        FilePath f = jenkins.getInstance().getWorkspaceFor(job)
        owner.getRootDir() >> new File(f.getRemote())

        GroovySpy(StepWrapperFactory, global:true)
        new StepWrapperFactory(_) >> Mock(StepWrapperFactory){
            createFromFilePath(*_) >> { args ->
                String name = args[0].getBaseName()
                return new StepWrapper(name)
            }
        }

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(job, scm)
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

        WorkflowJob job = jenkins.createProject(WorkflowJob)
        FilePath f = jenkins.getInstance().getWorkspaceFor(job)
        owner.getRootDir() >> new File(f.getRemote())

        GroovySpy(StepWrapperFactory, global:true)
        new StepWrapperFactory(_) >> Mock(StepWrapperFactory){
            createFromFilePath(*_) >> { args ->
                String name = args[0].getBaseName()
                return new StepWrapper(name)
            }
        }

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(job, scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        def binding = new Binding()

        when:
        p.loadLibrary(owner, binding, libraryName, [:])

        then:
        1 * logger.println("[JTE] Loading Library someLibrary")
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

    GitSCM createSCM(GitSampleRepoRule _repo){
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
