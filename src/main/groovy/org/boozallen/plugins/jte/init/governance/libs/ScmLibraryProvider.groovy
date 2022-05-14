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

import hudson.Extension
import hudson.FilePath
import hudson.Util
import hudson.scm.SCM
import jenkins.scm.api.SCMFile
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.util.FileSystemWrapperFactory
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

/**
 * fetches libraries from a source code repository
 */
class ScmLibraryProvider extends LibraryProvider{

    SCM scm
    String baseDir

    // jenkins requires this be here
    @SuppressWarnings('UnnecessaryConstructor')
    @DataBoundConstructor
    ScmLibraryProvider(){}

    @DataBoundSetter
    void setBaseDir(String baseDir) {
        this.baseDir = Util.fixEmptyAndTrim(baseDir)
    }

    String getBaseDir() { return baseDir }

    @DataBoundSetter
    void setScm(SCM scm){ this.scm = scm }

    SCM getScm(){ return scm }

    @Override
    Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName){
        SCMFileSystem fs = createFs(flowOwner)
        if (!fs){ return false }
        SCMFile lib = fs.child(prefixBaseDir(libName))

        if( !lib.isDirectory() ){
            return false
        }

        SCMFile stepsDir = lib.child(LibraryProvider.STEPS_DIR_NAME)
        boolean hasSteps = null != stepsDir && stepsDir.isDirectory()

        SCMFile srcDir = lib.child(LibraryProvider.SRC_DIR_NAME)
        boolean hasClasses = null != srcDir && srcDir.isDirectory()

        if( !hasSteps && !hasClasses ){
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.printWarning("Library ${libName} exists but does not have a '${LibraryProvider.STEPS_DIR_NAME}' or '${LibraryProvider.SRC_DIR_NAME}' directory. Library will not be loaded.")
        }
        return (hasSteps || hasClasses)
    }

    @Override
    String getLibrarySchema(FlowExecutionOwner flowOwner, String libName){
        SCMFileSystem fs = createFs(flowOwner)
        SCMFile lib = fs.child(prefixBaseDir(libName))
        SCMFile libConfigFile = lib.child(CONFIG_FILE)
        if(libConfigFile.exists() && libConfigFile.isFile()){
            return libConfigFile.contentAsString()
        }
        return null
    }

    @Override
    void loadLibrary(FlowExecutionOwner flowOwner, String libName, FilePath srcDir, FilePath libDir){
        // log the library being loaded to the build log
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        ArrayList msg = [
                "Loading Library ${libName}",
                "-- scm: ${scm.getKey()}"
        ]
        logger.print(msg.join("\n"))

        SCMFileSystem fs = createFs(flowOwner)
        // we already know this exists from hasLibrary()
        SCMFile lib = fs.child(prefixBaseDir(libName))

        // copy src files into srcDir
        SCMFile src = lib.child(LibraryProvider.SRC_DIR_NAME)
        recurseChildren(src){ file ->
            String relativePath = file.getPath() - "${prefixBaseDir(libName)}/"
            FilePath srcFile = srcDir.child(relativePath)
            if(srcFile.exists()){
                throw new JTEException("Source file ${relativePath} already exists. Check across libraries for duplicate class definitions.")
            }
            srcFile.write(file.contentAsString(), "UTF-8")
        }

        /*
         * recurse through the steps directory in the remote repository
         * for each groovy file, copy to the build's directory represented
         * by a FilePath
         */
        SCMFile steps = lib.child(LibraryProvider.STEPS_DIR_NAME)
        recurseChildren(steps){ file ->
            if(file.getName().endsWith(".groovy")) {
                String relativePath = file.getPath() - "${prefixBaseDir(libName)}/"
                FilePath stepFile = libDir.child(relativePath)
                stepFile.write(file.contentAsString(), "UTF-8")
            }
        }

        /*
         * For each resource file in the remote repository, copy it into the
         * build dir maintaining the file structure
         */
        SCMFile resources = lib.child(LibraryProvider.RESOURCES_DIR_NAME)
        recurseChildren(resources){ file ->
            String relativePath = file.getPath() - "${prefixBaseDir(libName)}/"
            FilePath resourceFile = libDir.child(relativePath)
            resourceFile.write(file.contentAsString(), "UTF-8")
        }
    }

    void recurseChildren(SCMFile file, Closure action){
        if(file.exists()){
            file.children().each{ child ->
                if(child.isDirectory()){
                    recurseChildren(child, action)
                }
                if(child.isFile()){
                    action(child)
                }
            }
        }
    }

    @SuppressWarnings('ImplicitClosureParameter')
    String prefixBaseDir(String s){
        return [baseDir, s?.trim()].findAll{ it }.join("/")
    }

    // TODO: lol CodeNarc is probably right and we should find a new place for this
    @SuppressWarnings('FactoryMethodName')
    SCMFileSystem createFs(FlowExecutionOwner flowOwner){
        return FileSystemWrapperFactory.create(flowOwner, scm) as SCMFileSystem
    }

    @Extension
    static class DescriptorImpl extends LibraryProvider.LibraryProviderDescriptor{
        String getDisplayName(){
            return "From SCM"
        }
    }

}
