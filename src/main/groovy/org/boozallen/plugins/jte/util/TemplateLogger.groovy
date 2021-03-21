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
import hudson.MarkupText
import hudson.console.ConsoleAnnotationDescriptor
import hudson.console.ConsoleAnnotator
import hudson.console.ConsoleNote
import hudson.model.TaskListener
import org.codehaus.groovy.runtime.InvokerHelper
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import java.security.SecureRandom

/**
 * Different log levels used within the {@link TemplateLogger} to change log color
 */
enum LogLevel{

    /**
     * grey
     */
    INFO(tag: "info"),
    /**
     * orange
     */
    WARN(tag: "warn"),
    /**
     * red
     */
    ERROR(tag: "error")
    String tag

}

/**
 * A utility for logging to a run's console output.
 */
class TemplateLogger {

    private static final String CONSOLE_NOTE_PREFIX = "[JTE]"
    TaskListener listener
    PrintStream logger

    TemplateLogger(TaskListener listener){
        this.listener = listener
        this.logger = listener.getLogger()
    }

    static TemplateLogger createDuringRun(){
        CpsThread thread = CpsThread.current()
        FlowExecution execution = thread?.getExecution()
        FlowExecutionOwner owner = execution?.getOwner()
        TaskListener listener = owner?.getListener()
        if(!listener){
            throw new Exception("Unable to find current TaskListener.  Can't create a TemplateLogger.")
        }
        TemplateLogger logger = new TemplateLogger(listener)
        return logger
    }

    // used for static calls to print methods
    @SuppressWarnings('MethodName')
    static void $static_methodMissing(String name, Object args) {
        TemplateLogger logger = createDuringRun()
        InvokerHelper.getMetaClass(logger).invokeMethod(logger, name, args)
    }

    void print(String message, LogLevel logType = LogLevel.INFO){
        String messageToPrint = message.trim()
        if(!messageToPrint.startsWith("[")){
            messageToPrint = messageToPrint.split("\n").collect{ line ->
                " ${line}"
            }.join("\n")
        }
        Boolean isMultiline = messageToPrint.contains("\n")
        String messageId = generateMessageId()
        messageToPrint.eachLine{ line, i ->
            synchronized (logger) {
                listener.annotate(new Annotator(
                    logType: logType,
                    messageId: messageId,
                    firstLine: !i,
                    multiLine: isMultiline
                ))
                logger.println(CONSOLE_NOTE_PREFIX + line)
            }
        }
    }

    void printWarning(String message) {
        print(message, LogLevel.WARN)
    }

    void printError(String message) {
        print(message, LogLevel.ERROR)
    }

    private String generateMessageId(){
        List alphabet = (["a".."z"] + [0..9]).flatten()
        String messageId = (1..10).collect{ alphabet[ new SecureRandom().nextInt(alphabet.size()) ] }.join()
        return messageId
    }

    static class Annotator extends ConsoleNote<WorkflowRun>{
        String messageId
        LogLevel logType
        Boolean firstLine
        Boolean multiLine
        Boolean initiallyHidden = true

        @Override
        ConsoleAnnotator<?> annotate(WorkflowRun context, MarkupText text, int charPos) {
            List<String> tags = [
                "class='jte-${logType.tag}'",
                "jte-id='${messageId}'",
            ]
            if(multiLine){
                tags << "first-line='${firstLine}''"
                tags << "multi-line='${multiLine}'"
            }
            if(initiallyHidden){
                tags << "initially-hidden='${initiallyHidden}'"
            }
            text.wrapBy("<span ${tags.join(" ")}>", '</span>')
            return null
        }

        @Extension
        static final class DescriptorImpl extends ConsoleAnnotationDescriptor {}
    }

}
