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
package org.boozallen.plugins.jte.init.governance.config.dsl

import hudson.EnvVars
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * used to get environment variables
 */
class DslEnvVar implements Serializable{

    private static final long serialVersionUID = 1L

    FlowExecutionOwner flowOwner

    DslEnvVar(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
    }

    Object getProperty(String property){
        TaskListener listener = flowOwner.getListener()
        EnvVars env = flowOwner.run().getEnvironment(listener)
        return env.get(property, null)
    }

}
