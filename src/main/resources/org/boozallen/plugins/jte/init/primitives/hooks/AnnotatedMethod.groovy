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

class AnnotatedMethod implements Serializable{
    Annotation annotation
    String annotationName
    def stepWrapper 
    String methodName

    AnnotatedMethod(Annotation annotation, String annotationName, String methodName, def stepWrapper){
        this.annotation = annotation
        this.annotationName = annotationName
        this.methodName = methodName
        this.stepWrapper = stepWrapper 
    } 

    void invoke(HookContext context){
        try{
            String lib = stepWrapper.library
            String step = stepWrapper.name 
            TemplateLogger.createDuringRun().print "[@${annotationName} - ${lib}/${step}.${methodName}]"
            InvokerHelper.getMetaClass(stepWrapper.impl).invokeMethod(stepWrapper.impl, methodName, context)
        } catch (Exception x) {
            throw new InvokerInvocationException(x)
        }
    }
}   