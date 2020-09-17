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

import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.TemplateConfigException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * base class for different mechanisms to load a library
 */
abstract class LibraryProvider extends AbstractDescribableImpl<LibraryProvider>{

    public static final String CONFIG_FILE = "library_config.groovy"

    /*
        implementing methods return true if library is present
        and false if not.
    */
    abstract Boolean hasLibrary(FlowExecutionOwner flowOwner, String libraryName)

    /*
        implementing methods should check for the existence of
        CONFIG_FILE in the library and pass file contents as string to
        doLibraryConfigValidation
    */
    abstract List loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig)

    List<String> doLibraryConfigValidation(FlowExecutionOwner flowOwner, String configFile, Map libConfig){
        return new ConfigChecker().doConfigFileValidation(flowOwner, configFile, libConfig)
    }

    /**
     * an instance can be reused within an injector call but not across multiple injector types
     */
    static class ConfigChecker {
        int padding = 3

        void printErrors(List<String> errors, FlowExecutionOwner flowOwner, String heading, String errorMsg = null){
            List<String> outputs = errors.flatten() - null
            String exceptionMsg = errorMsg ?: "There were ${heading.toLowerCase()}.".toString()

            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

            // if errors were found:
            if(outputs){
                String spacing = " " * padding
                String dashes = "-" * (heading.size() + (2 * padding)) // heading padded with 3 on each side
                logger.printError(dashes)
                logger.printError("${spacing}${heading}${spacing}")
                logger.printError(dashes)
                outputs.each{ line ->
                    logger.printError(line)
                }
                logger.printError(dashes)
                throw new TemplateConfigException(exceptionMsg)
            }
        }

        List<String> doConfigFileValidation(FlowExecutionOwner flowOwner, String configFile, Map libConfig) {
            PipelineConfigurationDsl dsl = new PipelineConfigurationDsl(flowOwner)
            Map allowedConfig = dsl.parse(configFile).getConfig()

            return doConfigMapValidation(flowOwner, allowedConfig, libConfig)
        }

        @SuppressWarnings('NoDef')
        List<String> doConfigMapValidation(FlowExecutionOwner flowOwner, Map allowedConfig, Map libConfig){
            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

            ArrayList libConfigErrors = []

            // define keysets in dot notation
            ArrayList keys = getNestedKeys(libConfig).collect{ key -> key.toString() }
            ArrayList required = getNestedKeys(allowedConfig.fields.required).collect{ key -> key.toString() }
            ArrayList optional = getNestedKeys(allowedConfig.fields.optional).collect{ key -> key.toString() }

            // validate required keys
            required.each{ requiredKey  ->
                if(requiredKey in keys){
                    keys -= requiredKey
                    def actual = getProp(libConfig, requiredKey)
                    def expected = getProp(allowedConfig.fields.required, requiredKey)
                    if (!validateType(logger, actual, expected)){
                        if (expected instanceof java.util.regex.Pattern){
                            libConfigErrors << "Field ${requiredKey} must be a String matching ${expected} but is [${actual}]"
                        } else if (expected instanceof ArrayList){
                            libConfigErrors << "Field '${requiredKey}' must be one of ${expected} but is [${actual}]"
                        } else {
                            libConfigErrors << "Field '${requiredKey}' must be a ${expected.getSimpleName()} but is a ${actual.getClass().getSimpleName()}"
                        }
                    }
                } else{
                    libConfigErrors << "Missing required field '${requiredKey}'"
                }
            }

            // validate optional keys
            optional.each{ optionalKey ->
                if(optionalKey in keys){
                    keys -= optionalKey
                    def actual = getProp(libConfig, optionalKey)
                    def expected = getProp(allowedConfig.fields.optional, optionalKey)
                    if (!validateType(logger, actual, expected)){
                        if (expected instanceof java.util.regex.Pattern){
                            libConfigErrors << "Field ${optionalKey} must be a String matching ${expected} but is [${actual}]"
                        } else if (expected instanceof ArrayList){
                            libConfigErrors << "Field '${optionalKey}' must be one of ${expected} but is [${actual}]"
                        } else {
                            libConfigErrors << "Field '${optionalKey}' must be a ${expected.getSimpleName()} but is a ${actual.getClass().getSimpleName()}"
                        }
                    }
                }
            }

            // validate that there are no extraneous keys
            keys.each{ key ->
                libConfigErrors << "Field '${key}' is not used."
            }

            return libConfigErrors
        }

        @SuppressWarnings(['NoDef', 'MethodReturnTypeRequired'])
        def getProp(LinkedHashMap o, String p){
            return p.tokenize('.').inject(o){ obj, prop ->
                obj?."$prop"
            }
        }

        List<String> getNestedKeys(LinkedHashMap map, List<String> result = [], String keyPrefix = '') {
            map.each { key, value ->
                if (value instanceof Map) {
                    getNestedKeys(value, result, "${keyPrefix}${key}.")
                } else {
                    result << "${keyPrefix}${key}"
                }
            }
            return result
        }

        /*
            In general here, we're looking to validate intent
            over specifics of what class they want.  It's unlikely
            the difference between boolean or Boolean, or Double
            vs BigDecimal vs Float will make a difference for a
            JTE configuration file and we should strive to avoid
            confusion when people specify a validation.
        */
        @SuppressWarnings(['MethodParameterTypeRequired', 'NoDef'])
        Boolean validateType(TemplateLogger logger, actual, expected){
            switch(expected){
                case [ boolean, Boolean ]:
                    return actual.getClass() in [ boolean, Boolean ]
                case String:
                    return actual.getClass() in [ String,  org.codehaus.groovy.runtime.GStringImpl ]
                case [ Integer, int]:
                    return actual.getClass() in [ Integer, int ]
                case [ Double, BigDecimal, Float ]:
                    return actual.getClass() in [ Double, BigDecimal, Float ]
                case Number:
                    return actual instanceof Number
                case { expected instanceof java.util.regex.Pattern }:
                    if(!(actual.getClass() in [ String,  org.codehaus.groovy.runtime.GStringImpl ])){
                        return false
                    }
                    return actual.matches(expected)
                case { expected instanceof ArrayList }:
                    return actual in expected
                default:
                    logger.printWarning("Library Validator: Not sure how to handle value ${expected} with class ${expected.class}")
                    return true
            }
        }

    }

    static class LibraryProviderDescriptor extends Descriptor<LibraryProvider> {}

}
