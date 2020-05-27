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

class ScmLibraryProvider extends LibraryProvider{

    public SCM scm
    public String baseDir

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

    Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName){
        SCMFileSystem fs = createFs(flowOwner)
        if (!fs) return false
        SCMFile lib = fs.child(prefixBaseDir(libName))
        return lib.isDirectory()
    }

    List loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig){
        SCMFileSystem fs = createFs(flowOwner)
        if (!fs){ return }

        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        ArrayList msg = [
            "Loading Library ${libName}",
            "-- scm: ${scm.getKey()}"
        ]
        logger.print(msg.join("\n"))

        SCMFile lib = fs.child(prefixBaseDir(libName))

        // do validation if the library configuration file is present
        SCMFile libConfigFile = lib.child(CONFIG_FILE)
        ArrayList libConfigErrors = []
        if(libConfigFile.exists() && libConfigFile.isFile()){
            libConfigErrors = doLibraryConfigValidation(flowOwner, libConfigFile.contentAsString(), libConfig)
            if(libConfigErrors){
                return [ "${libName}:" ] + libConfigErrors.collect{ " - ${it}" }
            }
        }else{
            logger.printWarning("Library ${libName} does not have a configuration file.")
        }

        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        lib.children().findAll{
            it.getName().endsWith(".groovy") &&
            !it.getName().endsWith("library_config.groovy") // exclude lib config file
        }.each{ stepFile ->
            def s = stepFactory.createFromFile(stepFile, libName, binding, libConfig)
            binding.setVariable(s.getName(), s)
        }

        return libConfigErrors
    }

    String prefixBaseDir(String s){
        return [baseDir, s?.trim()].findAll{ it }.join("/")
    }

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
