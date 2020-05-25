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
import jenkins.scm.api.*
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

class FileSystemWrapper {
    SCM scm
    SCMFileSystem fs
    String scmKey
    FlowExecutionOwner owner

    FileSystemWrapper(){}

    static FileSystemWrapper createFromSCM(FlowExecutionOwner owner, SCM scm){
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        fsw.fsFromSCM(scm)
        return fsw
    }

    static FileSystemWrapper createFromJob(FlowExecutionOwner owner){
        FileSystemWrapper fsw = new FileSystemWrapper(owner: owner)
        WorkflowJob job = owner.run().getParent()
        fsw.fsFrom(job)
        return fsw
    }

    /*
        retrieves a SCMFile if present.
        if ignoreMissing = true, missing files arent logged.
        returns null if file not present
    */
    String getFileContents(String filePath, String loggingDescription = null, Boolean logMissingFile = true) {
        if (!fs) {
            return null
        }
        TemplateLogger logger = new TemplateLogger(owner.getListener())
        try {
            SCMFile f = fs.child(filePath)
            if (!f.exists()) {
                if (logMissingFile) {
                    ArrayList msg = [
                        "${filePath} does not exist.",
                        "-- scm: ${scmKey}"
                    ]
                    logger.printWarning(msg.join("\n"))
                }
                return null
            }
            if (!f.isFile()) {
                ArrayList msg = [
                    "${filePath} exists but is not a file.",
                    "-- scm: ${scmKey}"
                ]
                logger.printWarning(msg.join("\n"))
                return null
            }
            if (loggingDescription){
                ArrayList msg = [
                    "Obtained ${loggingDescription}",
                    "-- scm: ${scmKey}",
                    "-- file path: ${filePath}"
                ]
                logger.print(msg.join("\n"))
            }

            return f.contentAsString()
        } catch(java.io.FileNotFoundException fne){
            if (logMissingFile) {
                ArrayList msg = [
                    "${filePath} threw FileNotFoundException.",
                    "-- scm: ${scmKey}"
                ]
                logger.printWarning(msg.join("\n"))
            }
            return null
        }
        finally {
            fs.close()
        }

    }

    def fsFromSCM(SCM scm){
        WorkflowJob job = owner.run().getParent()
        if(!scm || !job){
            return [null, null]
        }

        try{
            scmKey = scm.getKey()
            fs = SCMFileSystem.of(job,scm)
            return [fs, scmKey]
        }catch(any){
            new TemplateLogger(owner.getListener()).printWarning(any.toString())
            return [null, null]
        }
    }

    /*
        return[0]: SCMFileSystem
        return[1]: String: key from scm
    */
    def fsFrom(WorkflowJob job){
        ItemGroup<?> parent = job.getParent()
        TaskListener listener = owner.getListener()

        try {
            if (parent instanceof WorkflowMultiBranchProject) {
                // ensure branch is defined
                BranchJobProperty property = job.getProperty(BranchJobProperty.class)
                if (!property) {
                    throw new JTEException("inappropriate context") // removed IllegalStateEx as an example
                }
                Branch branch = property.getBranch()

                // get scm source for specific branch and ensure present
                // (might not be if branch deleted after job triggered)
                SCMSource scmSource = parent.getSCMSource(branch.getSourceId())
                if (!scmSource) {
                    throw new JTEException(new IllegalStateException("${branch.getSourceId()} not found"))
                }

                SCMHead head = branch.getHead()
                SCMRevision tip = scmSource.fetch(head, listener)

                scmKey = branch.getScm().getKey()

                if (tip) {
                    SCMRevision rev = scmSource.getTrustedRevision(tip, listener)
                    fs = SCMFileSystem.of(scmSource, head, rev)
                    return [fs, scmKey]
                } else {
                    SCM scm = branch.getScm()
                    fs = SCMFileSystem.of(job, scm)
                    return [fs, scmKey]
                }
            } else {
                FlowDefinition definition = job.getDefinition()
                if (definition instanceof CpsScmFlowDefinition) {
                    SCM scm = definition.getScm()
                    scmKey = scm.getKey()
                    fs = SCMFileSystem.of(job, scm)
                    return [fs, scmKey]
                } else {
                    return [fs, scmKey]
                }
            }
        }catch(JTEException jteex){//throw our exception
            throw (jteex.cause ?: jteex)
        }catch(any){// ignore but print every other exception
            new TemplateLogger(listener).printWarning(any.toString())
        }

        return [fs, scmKey]
    }

    Object asType(Class clazz) {
        if( null != fs && clazz.isInstance(fs)){
            return fs
        }

        if( clazz.isInstance(this)){
            return this
        }

        return null
    }

}
