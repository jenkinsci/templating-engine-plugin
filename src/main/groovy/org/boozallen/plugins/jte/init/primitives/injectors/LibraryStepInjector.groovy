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
import hudson.FilePath
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvider
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.ConfigValidator
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Loads libraries from the pipeline configuration
 */
@Extension class LibraryStepInjector extends TemplatePrimitiveInjector{

    private static final String KEY = "libraries"

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
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config){
        // fetch library providers and determine library resolution order
        List<LibraryProvider> providers = getLibraryProviders(flowOwner)
        boolean reverseProviders = config.jteBlockWrapper.reverse_library_resolution
        if(reverseProviders) {
            providers = providers.reverse()
        }

        // prepare directory to store loaded libraries
        FilePath buildRootDir = new FilePath(flowOwner.getRootDir())
        FilePath jteDir = buildRootDir.child("jte")

        // load all the libraries
        // this will copy their contents to ${jteDir} for the run
        LinkedHashMap aggregatedConfig = config.getConfig()
        aggregatedConfig[KEY].each{ libName, libConfig ->
            LibraryProvider provider = providers.find{ provider ->
                provider.hasLibrary(flowOwner, libName)
            }
            FilePath libDir = jteDir.child(libName)
            libDir.mkdirs()
            provider.loadLibrary(flowOwner, libName, jteDir, libDir)
        }

        // actually create the StepWrappers
        LibraryNamespace libCollector = new LibraryNamespace()
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        aggregatedConfig[KEY].each{ libName, libConfig ->
            String includes = "${libName}/${LibraryProvider.STEPS_DIR_NAME}/**/*.groovy"
            TemplatePrimitiveNamespace library = new TemplatePrimitiveNamespace(name: libName)
            library.setParent(libCollector)
            jteDir.list(includes).each{ stepFile ->
                StepWrapper step = stepFactory.createFromFilePath(stepFile, libName, libConfig)
                step.setParent(library)
                library.add(step)
            }
            if(library.getPrimitives()) {
                libCollector.add(library)
            }
        }

        if(libCollector.getLibraries()) {
            TemplatePrimitiveCollector primitiveCollector = getPrimitiveCollector(flowOwner)
            primitiveCollector.addNamespace(libCollector)
            flowOwner.run().addOrReplaceAction(primitiveCollector)
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

}
