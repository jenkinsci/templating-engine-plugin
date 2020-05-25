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

package org.boozallen.plugins.jte.init.governance.config

import hudson.model.TaskListener
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.scm.NullSCM
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.TestFlowExecutionOwner
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification


class ScmPipelineConfigurationProviderSpec extends Specification{

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

    /************************************
     *  tests for getConfig
     ************************************/

    def "getConfig: returns null if scm not defined"(){
        setup: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 

        expect: 
        p.getConfig(owner) == null 
    }

    def "getConfig: returns null if scm is NullSCM"(){
        setup: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        NullSCM scm = Mock()
        p.setScm(scm)

        expect: 
        p.getConfig(owner) == null 
    }

    def "getConfig: repository root: returns null if absent"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        repo.init() 
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> Mock(EnvActionImpl) 

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getConfig(owner) == null
    }
    
    def "getConfig: repository root: returns parsed config if present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        repo.init() 
        repo.write(ScmPipelineConfigurationProvider.CONFIG_FILE, "x = 1")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> Mock(EnvActionImpl) 

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getConfig(owner).config == [x: 1]
    }

    def "getConfig: base directory: returns null if absent"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        p.setBaseDir("pipeline-configuration")
        repo.init() 
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> Mock(EnvActionImpl) 

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getConfig(owner) == null
    }
    
    def "getConfig: base directory: returns parsed config if present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        repo.init()
        String baseDir = "pipeline-configuration"
        p.setBaseDir(baseDir)
        String path = "${baseDir}/${ScmPipelineConfigurationProvider.CONFIG_FILE}"  
        repo.write(path, "x = 1")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> Mock(EnvActionImpl) 

        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getConfig(owner).config == [x: 1]
    }

    def "getConfig: failure to parse config file prints error"(){
        given:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        p.setScm(Mock(GitSCM))

        FileSystemWrapper fsw = Mock{
            getFileContents(*_) >> "mock file contents"
        }
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(*_) >> fsw

        PipelineConfigurationDsl dsl = Mock{
            parse(_) >> { throw new Exception("oops") }
        }
        GroovySpy(PipelineConfigurationDsl, global: true)
        new PipelineConfigurationDsl(_) >> dsl
  
        when:
        p.getConfig(owner)

        then:
        thrown(Exception)
        1 * logger.println("[JTE] Error parsing scm provided pipeline configuration")
    }

    /************************************
     *  tests for getJenkinsfile
     ************************************/

    def "getJenkinsfile: returns null if scm not defined"(){
        setup: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 

        expect: 
        p.getJenkinsfile(owner) == null 
    }

    def "getJenkinsfile: returns null if scm is NullSCM"(){
        setup: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        NullSCM scm = Mock()
        p.setScm(scm)

        expect: 
        p.getJenkinsfile(owner) == null 
    }

    def "getJenkinsfile: no basedir returns null if not present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        repo.init()
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getJenkinsfile(owner) == null
    }

    def "getJenkinsfile: no basedir returns Jenkinsfile if present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        repo.init()
        repo.write("Jenkinsfile", "the jenkinsfile")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getJenkinsfile(owner) == "the jenkinsfile"
    }

    def "getJenkinsfile: basedir returns Jenkinsfile if present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        String baseDir = "pipeline-configuration"
        p.setBaseDir(baseDir)
        repo.init()
        repo.write("${baseDir}/Jenkinsfile", "the jenkinsfile")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getJenkinsfile(owner) == "the jenkinsfile"
    }

    def "getJenkinsfile: basedir returns null if not present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        String baseDir = "pipeline-configuration"
        p.setBaseDir(baseDir)
        repo.init()
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getJenkinsfile(owner) == null
    }

     /************************************
     *  tests for getTemplate
     ************************************/
    def "getTemplate: returns null if scm not defined"(){
        setup: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 

        expect: 
        p.getTemplate(owner, "someTemplate") == null 
    }

    def "getTemplate: returns null if scm is NullSCM"(){
        setup: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        NullSCM scm = Mock()
        p.setScm(scm)

        expect: 
        p.getTemplate(owner, "someTemplate") == null 
    }

    def "getTemplate: no basedir returns null if not present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        repo.init()
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getTemplate(owner, "someTemplate") == null
    }

    def "getTemplate: no basedir returns template if present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        repo.init()
        repo.write("pipeline_templates/someTemplate", "the template")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getTemplate(owner, "someTemplate") == "the template"
    }

    def "getTemplate: basedir returns template if present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        String baseDir = "pipeline-configuration"
        p.setBaseDir(baseDir)
        repo.init()
        repo.write("${baseDir}/pipeline_templates/someTemplate", "the template")
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getTemplate(owner, "someTemplate") == "the template"
    }

    def "getTemplate: basedir returns null if not present"(){
        given: 
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider() 
        String baseDir = "pipeline-configuration"
        p.setBaseDir(baseDir)
        repo.init()
        GitSCM scm = createSCM(repo)
        p.setScm(scm)
        
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fs = SCMFileSystem.of(jenkins.createProject(WorkflowJob), scm)
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(owner, scm) >> fsw

        expect: 
        p.getTemplate(owner, "someTemplate") == null
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