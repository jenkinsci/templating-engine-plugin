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
import org.codehaus.groovy.runtime.GStringImpl
import org.codehaus.groovy.runtime.NullObject
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
                StepAlias alias = getStepAlias(step)
                if(shouldKeepOriginal(alias)) {
                    step.setParent(library)
                    library.add(step)
                }
                Set<String> stepAliases = retrieveStepAliases(step, alias, flowOwner)
                if(!stepAliases.isEmpty()) {
                    stepAliases.each{ aliasString ->
                        StepWrapper clone = step.clone()
                        clone.setParent(library)
                        StepContext stepContext = new StepContext(library: libName, name: aliasString, isAlias: true)
                        clone.setStepContext(stepContext)
                        library.add(clone)
                    }
                }
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

    /**
     * Determines whether or not to keep the original step based on @StepAlias values
     * default is true if no @StepAlias annotation is present
     * default is false if @StepAlias is present, but can be overridden by step author.
     *
     * @param step the step to inspect
     * @param flowOwner run
     * @return whether or not to keep the original step name
     */
    private boolean shouldKeepOriginal(StepAlias alias){
        return alias ? alias.keepOriginal() : true
    }

    private Set<String> retrieveStepAliases(StepWrapper step, StepAlias alias, FlowExecutionOwner flowOwner){
        if(alias == null) {
            return []
        }
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        List<String> aliasList = []
        // handle static aliases
        aliasList.addAll(alias.value())
        // handle dynamic aliases
        Binding b = new Binding([config: step.getScript().getConfig()])
        Closure c = alias.dynamic().newInstance(b, b)
        try{
            Object result = c.call()
            switch(result.getClass()){
                case String:
                    aliasList.add(result)
                    break
                case GStringImpl:
                    aliasList.add(result.toString())
                    break
                case Collection:
                    result.each{ r ->
                        if(r instanceof String){
                            aliasList.add(r)
                        } else if (r instanceof GStringImpl){
                            aliasList.add(r.toString())
                        } else {
                            throw new JTEException("@StepAlias Dynamic closure returned a collection with non-string element: ${result}")
                        }
                    }
                    break
                case NullObject:
                    break
                default:
                    throw new JTEException("@StepAlias Dynamic closure must return a string, received: ${result.getClass()}")
            }
        } catch(any){
            logger.printError("Error evaluating @StepAlias dynamic closure for the ${step.library}'s ${step.name} step")
            throw any
        }
        if(aliasList.isEmpty()){
            logger.printWarning("@StepAlias did not define any aliases for the ${step.library}'s ${step.name} step")
        }
        return aliasList
    }

    /**
     * Gets a @StepAlias if present
     * returns null if no @StepAlias
     * throws exception if multiple alias annotations
     */
    private StepAlias getStepAlias(StepWrapper step){
        List<StepAlias> annotations = step.getScript().class.methods.collect{ method ->
            method.getAnnotation(StepAlias)
        } - null
        int n = annotations.size()
        switch(n){
            case 0:
                return null
            case 1:
                return annotations.first()
            default:
                throw new JTEException("There can only be one @StepAlias annotation per step. Found ${n} in ${step.library} library's ${step.name}.groovy")
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
