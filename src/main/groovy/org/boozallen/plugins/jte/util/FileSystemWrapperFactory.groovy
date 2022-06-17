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
package org.boozallen.plugins.jte.util

import hudson.model.ItemGroup
import hudson.model.TaskListener
import hudson.scm.SCM
import jenkins.branch.Branch
import jenkins.scm.api.SCMFileSystem
import jenkins.scm.api.SCMHead
import jenkins.scm.api.SCMRevision
import jenkins.scm.api.SCMSource
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

/**
 * Creates FileSystemWrappers
 */
class FileSystemWrapperFactory {

    /**
     * Creates a FileSystemWrapper. Can either be provided an SCM directly
     * or try to infer the SCM from the current job
     * @param owner the current run
     * @param scm an optionally provided SCM
     * @return a FileSystemWrapper
     */
    static FileSystemWrapper create(FlowExecutionOwner owner, SCM scm = null){
        WorkflowRun run = owner.run()
        WorkflowJob job = run.getParent()
        if (job == null){
            throw new IllegalStateException("Job cannot be null when creating FileSystemWrapper")
        }

        FileSystemWrapper fsw
        ItemGroup<?> parent = job.getParent()
        FlowDefinition definition = job.getDefinition()
        if (scm) {
            fsw = this.fromSCM(owner, job, scm)
        } else if (parent instanceof WorkflowMultiBranchProject) {
            TaskListener listener = owner.getListener()
            fsw = this.fromMultiBranchProject(owner, job, listener)
        } else if (definition instanceof CpsScmFlowDefinition) {
            fsw = this.fromPipelineJob(owner, job)
        }

        if(fsw){
            if(fsw.fs == null){
                TemplateLogger logger = new TemplateLogger(owner.getListener())
                logger.printWarning("FileSystemWrapperFactory: Unable to create SCMFileSystem: job: ${job.getFullName()}, scm: ${scm}")
            }
            return fsw
        }

        throw new IllegalStateException("Unable to build a FileSystemWrapper")
    }

    private static FileSystemWrapper fromSCM(FlowExecutionOwner owner, WorkflowJob job, SCM scm){
        SCMFileSystem fs
        fs = SCMFileSystem.of(job, scm)
        FileSystemWrapper fsw = new FileSystemWrapper(fs: fs, scmKey: scm.getKey(), owner: owner)
        return fsw
    }

    private static FileSystemWrapper fromMultiBranchProject(FlowExecutionOwner owner, WorkflowJob job, TaskListener listener){
        ItemGroup<?> parent = job.getParent()

        BranchJobProperty property = job.getProperty(BranchJobProperty)
        if (!property) {
            throw new JTEException("BranchJobProperty is somehow missing")
        }
        Branch branch = property.getBranch()

        SCMSource scmSource = parent.getSCMSource(branch.getSourceId())
        if (!scmSource) {
            throw new IllegalStateException("${branch.getSourceId()} not found")
        }

        SCMHead head = branch.getHead()
        SCMRevision tip = scmSource.fetch(head, listener)

        SCMFileSystem fs
        String scmKey
        if (tip) {
            scmKey = branch.getScm().getKey()
            SCMRevision rev = scmSource.getTrustedRevision(tip, listener)
            fs = SCMFileSystem.of(scmSource, head, rev)
        } else {
            SCM jobSCM = branch.getScm()
            fs = SCMFileSystem.of(job, jobSCM)
            scmKey = jobSCM.getKey()
        }
        FileSystemWrapper fsw = new FileSystemWrapper(fs: fs, scmKey: scmKey, owner: owner)
        return fsw
    }

    private static FileSystemWrapper fromPipelineJob(FlowExecutionOwner owner, WorkflowJob job){
        FlowDefinition definition = job.getDefinition()
        SCM jobSCM = definition.getScm()
        String scmKey = jobSCM.getKey()
        SCMFileSystem fs = SCMFileSystem.of(job, jobSCM)
        FileSystemWrapper fsw = new FileSystemWrapper(fs: fs, scmKey: scmKey, owner: owner)
        return fsw
    }

}
