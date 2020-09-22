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
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.FileSystemWrapper
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
        return lib.isDirectory()
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
    void loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig){
        SCMFileSystem fs = createFs(flowOwner)
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        ArrayList msg = [
            "Loading Library ${libName}",
            "-- scm: ${scm.getKey()}"
        ]
        logger.print(msg.join("\n"))

        // we already know this exists from hasLibrary()
        SCMFile lib = fs.child(prefixBaseDir(libName))

        FilePath buildRootDir = new FilePath(flowOwner.getRootDir())
        FilePath rootDir = buildRootDir.child("jte/${libName}")
        rootDir.mkdirs()

        /*
         * recurse through the steps directory in the remote repository
         * for each groovy file, copy to the build's directory represented
         * by a FilePath
         */
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        SCMFile steps = lib.child("steps")
        recurseChildren(steps){ file ->
            if(file.getName().endsWith(".groovy")) {
                String relativePath = file.getPath() - "${prefixBaseDir(libName)}/"
                FilePath stepFile = rootDir.child(relativePath)
                stepFile.write(file.contentAsString(), "UTF-8")
                String stepName = file.getName() - ".groovy"
                binding.setVariable(stepName, stepFactory.createFromFilePath(
                    stepFile,
                    binding,
                    libName,
                    libConfig
                ))
            }
        }

        /*
         * For each resource file in the remote repository, copy it into the
         * build dir maintaining the file structure
         */
        SCMFile resources = lib.child("resources")
        recurseChildren(resources){ file ->
            String relativePath = file.getPath() - "${prefixBaseDir(libName)}/"
            FilePath resourceFile = rootDir.child(relativePath)
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

    // lol CodeNarc is probably right and we should find a new place for this
    @SuppressWarnings('FactoryMethodName')
    SCMFileSystem createFs(FlowExecutionOwner flowOwner){
        return FileSystemWrapper.createFromSCM(flowOwner, scm) as SCMFileSystem
    }

    @Extension
    static class DescriptorImpl extends LibraryProvider.LibraryProviderDescriptor{
        String getDisplayName(){
            return "From SCM"
        }
    }

}
