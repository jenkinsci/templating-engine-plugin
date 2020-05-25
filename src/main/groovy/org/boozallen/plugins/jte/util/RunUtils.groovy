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

import hudson.model.Queue
import hudson.model.TaskListener
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

class RunUtils implements Serializable{

    static TaskListener listener
    static FlowExecutionOwner owner
    static WorkflowRun build
    static PrintStream logger
    static WorkflowJob currentJob

    static CpsThread getCurrentThread(){
        return CpsThread.current() ?: {
            throw new IllegalStateException("CpsThread not present")
        }()
    }

    static FlowExecutionOwner getOwner(){
        CpsThread thread = getCurrentThread()
        FlowExecutionOwner owner = thread.getExecution().getOwner()
        return owner
    }

    static TaskListener getListener(){
        FlowExecutionOwner owner = getOwner()
        TaskListener listener = owner.getListener()
        return listener
    }

    static PrintStream getLogger(){
        TaskListener listener = getListener()
        PrintStream logger = listener.getLogger()
        return logger
    }

    static WorkflowRun getRun(){
        FlowExecutionOwner owner = getOwner()
        Queue.Executable exec = owner.getExecutable()
        if (!(exec instanceof WorkflowRun)) {
            throw new IllegalStateException("Must be run from a WorkflowRun, found: ${exec.getClass()}")
        }
        WorkflowRun run = (WorkflowRun) exec
        return run
    }

    static WorkflowJob getJob(){
        WorkflowRun run = getRun()
        WorkflowJob job = run.getParent()
        return job
    }

    static ClassLoader getClassLoader(){
        return Jenkins.get().pluginManager.uberClassLoader
    }
}
