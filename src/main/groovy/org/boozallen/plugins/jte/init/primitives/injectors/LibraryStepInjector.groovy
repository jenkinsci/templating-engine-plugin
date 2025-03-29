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
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.ConfigValidator
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.GStringImpl
import org.codehaus.groovy.runtime.NullObject
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Loads libraries from the pipeline configuration
 */
@Extension class LibraryStepInjector extends TemplatePrimitiveInjector{

    private static final String KEY = "libraries"

    @Override
    void validateConfiguration(CpsFlowExecution exec, PipelineConfigurationObject config){
        FlowExecutionOwner flowOwner = exec.getOwner()
        LinkedHashMap aggregatedConfig = config.getConfig()
        AggregateException errors = new AggregateException()
        List<LibraryProvider> providers = getLibraryProviders(flowOwner)
        boolean reverseProviders = config.jteBlockWrapper.reverse_library_resolution
        if(reverseProviders) {
            providers = providers.reverse()
        }
        aggregatedConfig[KEY].each { libName, libConfig ->
            LibraryProvider provider = providers.find{ provider ->
                provider.hasLibrary(flowOwner, libName)
            }
            if(provider){
                String schema = provider.getLibrarySchema(flowOwner, libName)
                if(schema){
                    try {
                        ConfigValidator validator = new ConfigValidator(flowOwner,  "Library ${libName}")
                        validator.validate(schema, libConfig)
                    } catch (AggregateException e) {
                        errors.add(e)
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
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config){
        FlowExecutionOwner flowOwner = exec.getOwner()

        // Get global tier libraries first
        GovernanceTier globalTier = TemplateGlobalConfig.get().getTier()
        Set<LibraryProvider> globalProviders = []
        if(globalTier?.getLibrarySources()) {
            globalProviders.addAll(globalTier.getLibrarySources().collect { source -> source.getLibraryProvider() } - null)
        }

        // fetch all library providers and determine library resolution order
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
        StepWrapperFactory stepFactory = new StepWrapperFactory(exec)
        aggregatedConfig[KEY].each{ libName, libConfig ->
            String includes = "${libName}/${LibraryProvider.STEPS_DIR_NAME}/**/*.groovy"
            TemplatePrimitiveNamespace library = new TemplatePrimitiveNamespace(name: libName)
            library.setParent(libCollector)

            // Find the provider for this library
            LibraryProvider provider = providers.find{ p -> p.hasLibrary(flowOwner, libName) }
            boolean isSandboxed = !globalProviders.contains(provider)

            jteDir.list(includes).each{ stepFile ->
                StepWrapper step = stepFactory.createFromFilePath(stepFile, libName, libConfig, isSandboxed)
                List<StepAlias> aliases = getStepAliases(step)
                if(aliases.isEmpty() || shouldKeepOriginal(aliases)) {
                    step.setParent(library)
                    library.add(step)
                }
                // fetch the aliases for the current step, if any
                Set<String> stepAliases = retrieveStepAliases(step, aliases, flowOwner)
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

        return libCollector.getPrimitives() ? libCollector : null
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
    private boolean shouldKeepOriginal(List<StepAlias> aliases){
        return true in aliases.collect{ alias -> alias.keepOriginal() }
    }

    /**
     * Returns a list of names to alias the current step to based on the availalbe
     * @StepAlias annotations present.
     * @param step the step to alias
     * @param aliases the list of @StepAlias annotations present
     * @param flowOwner the pipeline run
     * @return a Set of aliases
     */
    private Set<String> retrieveStepAliases(StepWrapper step, List<StepAlias> aliases, FlowExecutionOwner flowOwner){
        if(aliases.isEmpty()){
            return []
        }
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        Set<String> aliasList = []
        aliases.each { alias ->
            // handle static aliases
            aliasList.addAll(alias.value())
            // handle dynamic aliases
            Binding b = new Binding([config: step.getScript().getConfig()])
            Closure c = alias.dynamic().newInstance(b, b)
            try {
                Object result = c.call()
                switch (result.getClass()) {
                    case String:
                        aliasList.add(result)
                        break
                    case GStringImpl:
                        aliasList.add(result.toString())
                        break
                    case Collection:
                        result.each { r ->
                            if (r instanceof String) {
                                aliasList.add(r)
                            } else if (r instanceof GStringImpl) {
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
            } catch (any) {
                logger.printError("Error evaluating @StepAlias dynamic closure for the ${step.library}'s ${step.name} step")
                throw any
            }
            if (aliasList.isEmpty()) {
                logger.printWarning("@StepAlias did not define any aliases for the ${step.library}'s ${step.name} step")
            }
        }
        return aliasList
    }

    /**
     * Gets a @StepAlias if present
     * returns null if no @StepAlias
     * throws exception if multiple alias annotations
     */
    private List<StepAlias> getStepAliases(StepWrapper step){
        List<StepAlias> annotations = step.getScript().class.methods.collect{ method ->
            method.getAnnotation(StepAlias)
        } - null
        return annotations
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
