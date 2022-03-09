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

import jenkins.scm.api.SCMFile
import jenkins.scm.api.SCMFileSystem
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Utility class to simplify fetching files from remote source code repositories, if the SCM plugin has
 * implemented the <a href="https://github.com/jenkinsci/scm-api-plugin">Jenkins SCM API</a>
 */
class FileSystemWrapper {

    SCMFileSystem fs
    String scmKey
    FlowExecutionOwner owner

    @SuppressWarnings('ReturnNullFromCatchBlock')
    String getFileContents(String filePath, String loggingDescription = null, Boolean logMissingFile = true) {
        if (!fs) {
            return null
        }
        TemplateLogger logger = new TemplateLogger(owner.getListener())
        try {
            SCMFile f = fs.child(filePath)
            // The file does not exist
            if (!f.exists()) {
                if (logMissingFile) {
                    ArrayList msg = [
                        "${filePath} does not exist.",
                        "-- scm: ${scmKey}"
                    ]
                    logger.printWarning(msg.join("\n"))
                }
                return null
            }
            // the path is not a file
            if (!f.isFile()) {
                ArrayList msg = [
                    "${filePath} exists but is not a file.",
                    "-- scm: ${scmKey}"
                ]
                logger.printWarning(msg.join("\n"))
                return null
            }
            // the file exists!
            if (loggingDescription){
                ArrayList msg = [
                    "Obtained ${loggingDescription}",
                    "-- scm: ${scmKey}",
                    "-- file path: ${filePath}"
                ]
                logger.print(msg.join("\n"))
            }
            return f.contentAsString()
        } catch(FileNotFoundException ignored){
            if (logMissingFile) {
                ArrayList msg = [
                    "${filePath} threw FileNotFoundException.",
                    "-- scm: ${scmKey}"
                ]
                logger.printWarning(msg.join("\n"))
            }
            return null
        }
        finally {
            fs.close()
        }
    }

    Object asType(Class clazz) {
        if( null != fs && clazz.isInstance(fs)){
            return fs
        }

        if( clazz.isInstance(this)){
            return this
        }

        return null
    }

}
