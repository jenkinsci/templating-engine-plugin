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

import hudson.Extension
import jenkins.security.CustomClassFilter

/**
 * Permits JTE classes to be stored on a {@link org.jenkinsci.plugins.workflow.job.WorkflowRun} via the
 * {@link org.boozallen.plugins.jte.init.PipelineDecorator}
 * @see <a href="https://github.com/jenkinsci/jep/blob/master/jep/200/README.adoc#extensibility">JEP-200</a>
*/
@Extension
class CustomClassFilterImpl implements CustomClassFilter {

    @SuppressWarnings('BooleanMethodReturnsNull')
    @Override Boolean permits(Class<?> c){
        List<String> permitted = [ "org.boozallen.plugins.jte", "WorkflowScript" ]
        return permitted.find{ p -> c.getName().startsWith(p) } ? true : null
    }

}
