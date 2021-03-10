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
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.primitives.PrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.ConfigValidator
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Loads libraries from the pipeline configuration and injects StepWrapper's into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class LibraryStepInjector extends TemplatePrimitiveInjector {

    private static final String KEY = "libraries"
    private static final String TYPE_DISPLAY_NAME = "Library"
    private static final String NAMESPACE_KEY = KEY

    static PrimitiveNamespace createNamespace(){
        return new Namespace(name: getNamespaceKey(), typeDisplayName: TYPE_DISPLAY_NAME)
    }

    static String getNamespaceKey(){
        return NAMESPACE_KEY
    }

    @Override
    void validateConfiguration(FlowExecutionOwner flowOwner, PipelineConfigurationObject config){
        LinkedHashMap aggregatedConfig = config.getConfig()
        AggregateException errors = new AggregateException()
        List<LibraryProvider> providers = getLibraryProviders(flowOwner)
        boolean reverseProviders = config.jteBlockWrapper.reverse_library_resolution
        if(reverseProviders) {
            providers = providers.reverse()
        }
        ConfigValidator validator = new ConfigValidator(flowOwner)
        aggregatedConfig[KEY].each { libName, libConfig ->
            LibraryProvider provider = providers.find{ provider ->
                provider.hasLibrary(flowOwner, libName)
            }
            if(provider){
                String schema = provider.getLibrarySchema(flowOwner, libName)
                if(schema){
                    try {
                        validator.validate(schema, libConfig)
                    } catch (AggregateException e) {
                        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
                        String errorHeader = "Library ${libName} has configuration errors"
                        logger.printError(errorHeader)
                        e.getExceptions().eachWithIndex{ error, idx ->
                            logger.printError("${idx + 1}. ${error.getMessage()}".toString())
                        }
                        errors.add(new JTEException(errorHeader))
                    }
                }
            } else {
                errors.add(new JTEException("Library ${libName} not found."))
            }
        }
        if(errors.size()){
            throw errors
        }
    }

    @Override
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        LinkedHashMap aggregatedConfig = config.getConfig()

        List<LibraryProvider> providers = getLibraryProviders(flowOwner)
        boolean reverseProviders = config.jteBlockWrapper.reverse_library_resolution
        if(reverseProviders) {
            providers = providers.reverse()
        }

        LinkedHashMap providerCache = [:]
        aggregatedConfig[KEY].each{ libName, libConfig ->
            LibraryProvider provider = providers.find{ provider ->
                provider.hasLibrary(flowOwner, libName)
            }
            providerCache[libName] = provider
            provider.logLibraryLoading(flowOwner, libName)
            provider.loadLibraryClasses(flowOwner, libName)
        }

        /*
         * library steps need to be loaded in a second passthrough
         * to ensure that every library's source files have been
         * added to the classloader, lest compilation errors occur
         * during cross-library imports.
         *
         * TODO: there's probably a more efficient way to do this..
         */
        aggregatedConfig[KEY].each { libName, libConfig ->
            LibraryProvider provider = providerCache[libName]
            provider.loadLibrarySteps(flowOwner, binding, libName, libConfig)
        }
    }

    private List<LibraryProvider> getLibraryProviders(FlowExecutionOwner flowOwner){
        WorkflowJob job = flowOwner.run().getParent()
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(job)
        List<LibrarySource> librarySources = tiers.collect{ tier ->
            tier.getLibrarySources()
        }.flatten() - null
        List<LibraryProvider> providers = librarySources.collect{ source ->
            source.getLibraryProvider()
        } - null
        return providers
    }

    static class Namespace extends PrimitiveNamespace {
        String name = KEY
        List<CallableNamespace> libraries = []

        @Override
        void printAllPrimitives(TemplateLogger logger){
            logger.print( "created Library Steps:\n" + getFormattedVariables().join("\n") )
        }

        @Override void add(TemplatePrimitive primitive){
            String libName = primitive.getLibrary()
            CallableNamespace library = getLibrary(libName)
            if(!library){
                library = new CallableNamespace(name: libName)
                libraries.push(library)
            }
            library.add(primitive)
        }

        @Override Set<String> getVariables(){
            return libraries*.getVariables().flatten() as Set<String>
        }

        @Override List<String> getPrimitivesByName(String primitiveName){
            List<String> names = []
            libraries.each{ n ->
                String name = n.getPrimitivesByName(primitiveName)
                name && names.push(name)
            }
            return names.collect{ n -> "${name}.${n}" } as List<String>
        }

        Set<String> getFormattedVariables(){
            return libraries.collect{ lib ->
                lib.getVariables().collect { var ->
                    "${var} from the ${lib.name} Library"
                }
            }.flatten() as Set<String>
        }

        Object getProperty(String name){
            MetaProperty meta = getClass().metaClass.getMetaProperty(name)
            if(meta){
                return meta.getProperty(this)
            }

            CallableNamespace library = getLibrary(name)
            if(!library){
                throw new JTEException("Library ${name} not found.")
            }
            return library
        }
        private CallableNamespace getLibrary(String name){
            return libraries.find{ l -> l.getName() == name }
        }

    }

}
