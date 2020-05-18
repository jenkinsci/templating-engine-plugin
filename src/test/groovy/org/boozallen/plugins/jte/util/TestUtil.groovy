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

import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition
import org.jvnet.hudson.test.JenkinsRule

/**
 * provides common utility methods to tests 
 */
class TestUtil{

    /** 
     * creates a Pipeline Job with JTE via AdHocTemplateFlowDefinition 
     * @param args named parameters.  template and config expected keys
     * @param jenkins the JenkinsRule for the test
     */
    static WorkflowJob createAdHoc(LinkedHashMap args, JenkinsRule jenkins){
        WorkflowJob job = jenkins.createProject(WorkflowJob)
        def definition = new AdHocTemplateFlowDefinition(
            args.containsKey("template"), 
            args.template, 
            args.containsKey("config"), 
            args.config
        )
        job.setDefinition(definition)
        return job
    }

    /**
     * asserts the strings listed in @param order appear 
     * in the @param log in the specified order
     */
    static void assertOrder(String log, ArrayList order){
        def split = log.split("\n")
        def idxs = order.collect{ split.findIndexOf{ line -> line.contains(it) } }
        assert !idxs.contains(-1) : "At least 1 substring in ${order.inspect()} is not present in log:\n${log}"
        assert idxs == idxs.sort(false) : "substrings ${order.inspect()} do not appear in this order in: \n ${log}"
    }


}