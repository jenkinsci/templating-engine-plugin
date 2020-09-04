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
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.Extension
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.config.dsl.TemplateConfigException
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.primitives.JteNamespace
import org.boozallen.plugins.jte.init.primitives.JteNamespace.Namespace
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Loads libraries from the pipeline configuration and injects StepWrapper's into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class LibraryLoader extends TemplatePrimitiveInjector {

    static void populateNamespace(JteNamespace jte, TemplatePrimitive primitive){
        Namespace n = jte.getNamespace(key)
        if(!n) {
            // namespace doesn't exist yet.. create, push primitive, add
            n = new LibrariesNamespace()
            n.push(primitive)
            jte.addNamespace(n)
        } else if(!(n instanceof LibrariesNamespace)){
            // namespace exists but isn't from this injector somehow?
            throw new Exception("JTE Namespace conflict for name: ${key}")
        } else {
            // namespace exists.. just add primitive
            n.push(primitive)
        }
    }

    static class LibrariesNamespace extends Namespace {
        String name = getKey()
        List<Library> libraries = []
        @Override void push(TemplatePrimitive primitive){
            String libName = primitive.getLibrary()
            Library library = getLibrary(libName)
            if(!library){
                library = new Library(name: libName)
                libraries.push(library)
            }
            library.push(primitive)
        }
        Object getProperty(String name){
            Library library = getLibrary(name)
            if(!library){
                throw new Exception("Library ${name} not found.")
            }
            return library
        }
        Library getLibrary(String name){
            return libraries.find{ l -> l.getName() == name }
        }
        static class Library extends Namespace{
            String name
            List steps = []
            void push(TemplatePrimitive step){
                steps.push(step)
            }
            Object getProperty(String stepName){
                Object step = steps.find{ s -> s.getName() == stepName }
                if(!step){
                    throw new Exception("JTE Library ${name} does not have step: ${stepName}")
                }
                return step
            }
        }
    }

    static String getKey(){ "libraries" }

    @Override
    void doInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        // 1. Inject steps from loaded libraries
        WorkflowJob job = flowOwner.run().getParent()
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)
        List<LibrarySource> libs = tiers.collect{ tier ->
            tier.getLibrarySources()
        }.flatten() - null
        List<LibraryProvider> providers = libs.collect{ libSource ->
            libSource.getLibraryProvider()
        } - null

        LinkedHashMap aggregatedConfig = config.getConfig()
        def libraries = aggregatedConfig[getKey()]

        ArrayList libConfigErrors = []
        libraries.each{ libName, libConfig ->
            LibraryProvider p = providers.find{ provider ->
                provider.hasLibrary(flowOwner, libName)
            }
            if (p){
                libConfigErrors << p.loadLibrary(flowOwner, binding, libName, libConfig)
            } else {
                libConfigErrors << "Library ${libName} Not Found."
            }
        }
        libConfigErrors = libConfigErrors.flatten() - null

        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        // if library errors were found:
        if(libConfigErrors){
            logger.printError("----------------------------------")
            logger.printError("   Library Configuration Errors   ")
            logger.printError("----------------------------------")
            libConfigErrors.each{ line ->
                logger.printError(line)
            }
            logger.printError("----------------------------------")
            throw new TemplateConfigException("There were library configuration errors.")
        }

        // 2. Inject steps with default step implementation for configured step
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        config.getConfig().steps.findAll{ stepName, stepConfig ->
            if (binding.hasStep(stepName)){
                ArrayList msg = [
                    "Configured step ${stepName} ignored.",
                    "-- Loaded by the ${binding.getStep(stepName).library} Library."
                ]
                logger.printWarning msg.join("\n")
                return false
            }
            return true
        }.each{ stepName, stepConfig ->
            logger.print "Creating step ${stepName} from the default step implementation."
            binding.setVariable(stepName, stepFactory.createDefaultStep(binding, stepName, stepConfig))
        }
    }

    @Override
    void doPostInject(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, Binding binding){
        // 3. Inject a passthrough step for steps not defined (either as steps or other primitives)
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        config.getConfig().template_methods.findAll{ step ->
            !(step.key in binding.registry)
        }.each{ step ->
            binding.setVariable(step.key, stepFactory.createNullStep(step.key, binding))
        }
    }

}
