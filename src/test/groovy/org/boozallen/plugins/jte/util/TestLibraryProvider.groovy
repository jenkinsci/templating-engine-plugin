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
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

class TestLibraryProvider extends LibraryProvider{

    ArrayList<TestLibrary> libraries = []

    Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName){
        return libraries.find{ it.name == libName } as boolean
    }

    List loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig){
        FilePath buildRootDir = new FilePath(flowOwner.getRootDir())
        FilePath rootDir = buildRootDir.child("jte/${libName}")
        rootDir.mkdirs()
        if(hasLibrary(flowOwner, libName)){
            TestLibrary library = libraries.find{ it.name == libName }

            // copy resources
            library.resources.each{ path, contents ->
                FilePath resource = rootDir.child("resources/${path}")
                resource.write(contents, "UTF-8")
            }

            // load steps
            StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
            library.steps.each{ name, text ->
                FilePath step = rootDir.child("steps/${name}.groovy")
                step.write(text, "UTF-8")
                def s = stepFactory.createFromFilePath(step, binding, libName, libConfig)
                binding.setVariable(name, s)
            }
        }
        return []
    }
    
    void addStep(String libName, String stepName, String stepText){
        TestLibrary library = libraries.find{ it.name == libName }
        if(!library){
            library = new TestLibrary(name: libName)
            libraries << library 
        }
        library.addStep(stepName, stepText)
    }

    void addResource(String libName, String path, String resourceText){
        TestLibrary library = libraries.find{ it.name == libName }
        if(!library){
            library = new TestLibrary(name: libName)
            libraries << library
        }
        library.addResource(path, resourceText)
    }

    class TestLibrary {
        String name
        LinkedHashMap steps = [:]
        LinkedHashMap resources = [:]

        void addStep(String stepName, String text){
            if(steps.containsKey(stepName)){
                throw new Exception("Test Library ${name} already has step ${stepName}.")
            }
            steps[stepName] = text
        }

        void addResource(String path, String text){
            if(steps.containsKey(path)){
                throw new Exception("Test Library ${name} already has resource ${path}.")
            }
            resources[path] = text
        }
    }

    void addGlobally(){
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier()
        LibrarySource libSource = new LibrarySource(this)
        if(tier){
            tier.getLibrarySources() << libSource
        }else{
            tier = new GovernanceTier()
            List<LibrarySource> libSources = [ libSource ]
            tier.setLibrarySources(libSources)
            global.setTier(tier)
        }
        
    }

    @Extension static class DescriptorImpl extends LibraryProvider.LibraryProviderDescriptor{
        String getDisplayName(){
            return "From Unit Test"
        }
    }
}
