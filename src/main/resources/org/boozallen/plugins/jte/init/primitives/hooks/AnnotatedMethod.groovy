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
package org.boozallen.plugins.jte.init.primitives.hooks

import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException

import java.lang.annotation.Annotation

/**
 * Represents a method annotated within a step with a lifecycle hook
 */
@SuppressWarnings(["NoDef", "FieldTypeRequired"])
class AnnotatedMethod implements Serializable{

    private static final long serialVersionUID = 1L
    Annotation annotation
    String annotationName
    def stepWrapper
    String methodName

    @SuppressWarnings("MethodParameterTypeRequired")
    AnnotatedMethod(Annotation annotation, String annotationName, String methodName, def stepWrapper){
        this.annotation = annotation
        this.annotationName = annotationName
        this.methodName = methodName
        this.stepWrapper = stepWrapper
    }

    @SuppressWarnings("CatchException")
    void invoke(HookContext context){
        try{
            def step = stepWrapper.clone()
            def script = step.getScript()
            script.setHookContext(context)
            String lib = step.library
            String stepName = step.name
            TemplateLogger.createDuringRun().print "[@${annotationName} - ${lib}/${stepName}.${methodName}]"
            InvokerHelper.getMetaClass(script).invokeMethod(script, methodName, null)
        } catch (Exception x) {
            throw new InvokerInvocationException(x)
        }
    }

}
