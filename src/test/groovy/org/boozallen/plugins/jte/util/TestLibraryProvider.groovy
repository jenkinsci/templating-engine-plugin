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

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.Extension
import hudson.FilePath
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.TemplateConfigFolderProperty
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.util.JTEException
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

class TestLibraryProvider extends LibraryProvider {

    ArrayList<TestLibrary> libraries = []

    /**
     * Removes all global library sources
     */
    static void removeLibrarySources() {
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier()
        tier.setLibrarySources([])
    }

    /**
     * Removes all library sources from all Governance Tiers
     */
    static void wipeAllLibrarySources() {
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier()
        if (tier) {
            tier.setLibrarySources([])
        }
    }

    /**
     * Adds a class file to the provided library
     * @param libName the name of the library
     * @param path the path of the source file
     * @param content the source file contents
     */
    void addSrc(String libName, String path, String content) {
        TestLibrary library = getLibrary(libName)
        if (!library) {
            library = new TestLibrary(name: libName)
            libraries << library
        }
        library.addSrc(path, content)
    }

    /**
     * Adds a step file to the provided library
     * @param libName the name of the library
     * @param stepName the name of the step (without file extension)
     * @param stepText the source file contents
     */
    void addStep(String libName, String stepName, String stepText) {
        TestLibrary library = getLibrary(libName)
        if (!library) {
            library = new TestLibrary(name: libName)
            libraries << library
        }
        library.addStep(stepName, stepText)
    }

    /**
     * Adds a library resource to the provided library
     * @param libName the name of the library
     * @param path the local path of the resource within the resources directory
     * @param resourceText the source file contents
     */
    void addResource(String libName, String path, String resourceText) {
        TestLibrary library = getLibrary(libName)
        if (!library) {
            library = new TestLibrary(name: libName)
            libraries << library
        }
        library.addResource(path, resourceText)
    }

    /**
     * Adds a library_config.groovy file to the library
     * @param libName the name of the library
     * @param config the library configuration file contents
     */
    void addConfig(String libName, String config) {
        TestLibrary library = getLibrary(libName)
        if (!library) {
            library = new TestLibrary(name: libName)
            libraries << library
        }
        library.addConfig(config)
    }

    /**
     * Appends this library provider to a global library source
     */
    void addGlobally() {
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier()
        LibrarySource libSource = new LibrarySource(this)
        if (tier) {
            tier.getLibrarySources() << libSource
        } else {
            tier = new GovernanceTier()
            List<LibrarySource> libSources = [ libSource ]
            tier.setLibrarySources(libSources)
            global.setTier(tier)
        }
    }

    /**
     * Adds this library to the provided Folder
     * @param folder the folder to add the library source to
     */
    void addToFolder(Folder folder){
        TemplateConfigFolderProperty p = folder.getProperties().find{ p ->
            p instanceof TemplateConfigFolderProperty
        }
        LibrarySource libSource = new LibrarySource(this)
        if(p == null){
            GovernanceTier tier = new GovernanceTier()
            List<LibrarySource> libSources = [ libSource ]
            tier.setLibrarySources(libSources)
            p = new TemplateConfigFolderProperty(tier)
            folder.addProperty(p)
        } else {
            p.tier.getLibrarySources() << libSource
        }
    }

    TestLibrary getLibrary(String libName) {
        return libraries.find { lib -> lib.name == libName }
    }

    @SuppressWarnings('UnusedMethodParameter')
    @Override
    Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName) {
        return getLibrary(libName) as boolean
    }

    @Override
    void loadLibrary(FlowExecutionOwner flowOwner, String libName, FilePath srcDir, FilePath libDir) {
        if (hasLibrary(flowOwner, libName)) {
            TestLibrary library = getLibrary(libName)

            // copy src
            library.src.each { path, contents ->
                FilePath src = srcDir.child(path)
                if (src.exists()) {
                    throw new JTEException("src file '${path}' exists already exists")
                }
                src.write(contents, 'UTF-8')
            }

            // copy resources
            library.resources.each { path, contents ->
                FilePath resource = libDir.child("resources/${path}")
                resource.write(contents, 'UTF-8')
            }

            // load steps
            library.steps.each { name, text ->
                FilePath step = libDir.child("steps/${name}.groovy")
                step.write(text, 'UTF-8')
            }
        }
    }

    @Override
    String getLibrarySchema(FlowExecutionOwner flowOwner, String libName) {
        return hasLibrary(flowOwner, libName) ? getLibrary(libName).getConfig() : null
    }

    class TestLibrary {

        String name
        String config
        LinkedHashMap steps = [:]
        LinkedHashMap resources = [:]
        LinkedHashMap src = [:]

        void addStep(String stepName, String text) {
            if (steps.containsKey(stepName)) {
                throw new Exception("Test Library ${name} already has step ${stepName}.")
            }
            steps[stepName] = text
        }

        void addResource(String path, String text) {
            if (steps.containsKey(path)) {
                throw new Exception("Test Library ${name} already has resource ${path}.")
            }
            resources[path] = text
        }

        void addSrc(String path, String text) {
            if (src.containsKey(path)) {
                throw new Exception("Test Library ${name} already contains src ${path}.")
            }
            src[path] = text
        }

        void addConfig(String config) {
            this.config = config
        }

    }

    @Extension static class DescriptorImpl extends LibraryProvider.LibraryProviderDescriptor {

        String getDisplayName() {
            return 'From Unit Test'
        }

    }

}
