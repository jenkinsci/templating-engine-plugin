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
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun

/**
* to be mocked so Spock can find "run" method
*/
class TestFlowExecutionOwner extends FlowExecutionOwner {
    @Override FlowExecution get() { return null }
    @Override File getRootDir() { return null }
    @Override Queue.Executable getExecutable(){ return null }
    @Override String getUrl() { return null }
    @Override boolean equals(Object o) { return this==o }
    @Override int hashCode() { return 0 }
    WorkflowRun run(){ return null }
}