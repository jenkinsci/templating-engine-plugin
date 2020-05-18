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
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.util.TemplateLogger
import org.boozallen.plugins.jte.init.primitives.injectors.LibraryLoader
import hudson.Extension
import hudson.model.Descriptor
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import jenkins.model.Jenkins

class TestLibraryProvider extends LibraryProvider{

    ArrayList<TestLibrary> libraries = []

    Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName){
        return libraries.find{ it.name == libName } as boolean
    }

    List loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig){
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        if(hasLibrary(flowOwner, libName)){
            Class StepWrapper = LibraryLoader.getPrimitiveClass()
            TestLibrary library = libraries.find{ it.name == libName }
            library.steps.each{ name, text -> 
                def s = StepWrapper.createFromString(text, binding, name, libName, libConfig)
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

    class TestLibrary {
        String name
        LinkedHashMap steps = [:]

        void addStep(String stepName, String text){
            if(steps.containsKey(stepName)){
                throw new Exception("Test Library ${name} already has step ${stepName}.")
            }
            steps[stepName] = text
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
