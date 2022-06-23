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

import groovy.transform.InheritConstructors
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * A generic exception to mark an exception as coming from JTE
 */
@InheritConstructors class AggregateException extends Exception {

    private List<Exception> exceptions = []
    private String message

    void setMessage(String message){
        this.message = message
    }

    String getMessage(){
        return message
    }

    void add(Exception e){
        if(e instanceof AggregateException){
            exceptions += e.getExceptions()
        } else{
            exceptions.push(e)
        }
    }

    int size(){
        return exceptions.size()
    }

    List<Exception> getExceptions(){
        return exceptions
    }

    void printToConsole(CpsFlowExecution exec){
        FlowExecutionOwner flowOwner = exec.getOwner()
        TaskListener listener = flowOwner.getListener()
        TemplateLogger logger = new TemplateLogger(listener)
        exceptions.eachWithIndex{ e, i ->
            List<String> msg = [ "${i + 1}: ${e.getMessage()} "]
            msg.addAll(e.getStackTrace().collect{ s -> s.toString() })
            logger.printError(msg.join("\n"))
        }
    }

}
