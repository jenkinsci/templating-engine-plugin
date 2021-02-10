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

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Permits JTE classes to be stored on a {@link org.jenkinsci.plugins.workflow.job.WorkflowRun} via the
 * {@link org.boozallen.plugins.jte.init.PipelineDecorator}
 * @see <a href="https://github.com/jenkinsci/jep/blob/master/jep/200/README.adoc#extensibility">JEP-200</a>
*/
@Extension
class CustomClassFilterImpl implements CustomClassFilter {

    @SuppressWarnings("UnnecessaryDotClass")
    private static final Logger LOGGER = Logger.getLogger(CustomClassFilterImpl.class.getName())

    @SuppressWarnings("FieldName")
    private static final Set<String> APPROVED_CLASS_PARTIALS = [
        "WorkflowScript"
    ]

    @SuppressWarnings("FieldName")
    private static final Set<String> BINDING_CLASSES = []
    static void pushPermittedClass(Object o){
        String name = o.getClass().getName()
        if(!BINDING_CLASSES.contains(name)){
            LOGGER.log(Level.INFO, "Adding '${name}' to CustomClassFilterImpl")
        }
        BINDING_CLASSES.add(o.getClass().getName())
    }

    static void flush(){ BINDING_CLASSES.clear() }

    @SuppressWarnings('BooleanMethodReturnsNull')
    @Override Boolean permits(Class<?> c){
        return (c.getName() in BINDING_CLASSES || APPROVED_CLASS_PARTIALS.find{ clazz -> c.getName().contains(clazz) }) ? true : null
    }

}
