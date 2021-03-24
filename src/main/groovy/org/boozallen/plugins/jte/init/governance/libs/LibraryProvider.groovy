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
package org.boozallen.plugins.jte.init.governance.libs

import hudson.FilePath
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * base class for different mechanisms to load a library
 */
abstract class LibraryProvider extends AbstractDescribableImpl<LibraryProvider>{

    /**
     * the library configuration file path
     */
    public static final String CONFIG_FILE = "library_config.groovy"

    /**
     * the name of the directory containing step files
     */
    public static final String STEPS_DIR_NAME = "steps"

    /**
     * the name of the directory containing library resources
     */
    public static final String RESOURCES_DIR_NAME = "resources"

    /**
     * the name of the directory containing library src files
     */
    public static final String SRC_DIR_NAME = "src"

    /**
     * Determines whether the provider has a library
     * @param flowOwner the Run's FlowExecutionOwner
     * @param libName the Library to load
     * @return true if the provider has the library
     */
    abstract Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName)

    /**
     * Returns the contents of the library configuration file, if present.
     * Null otherwise
     *
     * @param flowOwner the Run's FlowExecutionOwner
     * @param libName the Library to load
     * @return the contents of the library configuration file, if present. null otherwise.
     */
    abstract String getLibrarySchema(FlowExecutionOwner flowOwner, String libName)

    /**
     * Copies the library's src files to the provided directory
     *
     * @param flowOwner the Run's FlowExecutionOwner
     * @param libName the library to load
     * @param srcDir the directory to copy src files to
     * @param libDir the directory to copy library setps to
     */
    abstract void loadLibrary(FlowExecutionOwner flowOwner, String libName, FilePath srcDir, FilePath libDir)

    static class LibraryProviderDescriptor extends Descriptor<LibraryProvider> {}

}
