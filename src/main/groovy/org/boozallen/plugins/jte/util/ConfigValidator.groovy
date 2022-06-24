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
package org.boozallen.plugins.jte.util

import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationDsl
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Utility to validate a map against a provided schema
 */
class ConfigValidator {

    FlowExecutionOwner flowOwner
    private final String msgPrefix

    ConfigValidator(FlowExecutionOwner flowOwner, String msgPrefix = null){
        this.flowOwner = flowOwner
        this.msgPrefix = msgPrefix
    }

    void validate(String schemaString, LinkedHashMap config){
        LinkedHashMap schema = parseSchema(schemaString)
        validate(schema, config)
    }

    @SuppressWarnings('NoDef')
    void validate(LinkedHashMap schema, LinkedHashMap config) throws AggregateException{
        // define key sets in dot notation
        List<String> keys = getNestedKeys(config)
        List<String> required = getNestedKeys(schema.fields?.required)
        List<String> optional = getNestedKeys(schema.fields?.optional)

        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        AggregateException errors = new AggregateException()

        // validate required keys
        required.each{ requiredKey  ->
            if(requiredKey in keys){
                keys -= requiredKey
                def actual = getProp(config, requiredKey)
                def expected = getProp(schema.fields?.required, requiredKey)
                boolean keyHasError = validateType(logger, actual, expected)
                if (keyHasError){
                    String msg
                    if (expected instanceof java.util.regex.Pattern){
                        msg = "field ${requiredKey} must be a String matching ${expected} but is [${actual}]"
                    } else if (expected instanceof ArrayList){
                        msg = "field '${requiredKey}' must be one of ${expected} but is [${actual}]"
                    } else {
                        msg = "field '${requiredKey}' must be a ${expected.getSimpleName()} but is a ${actual.getClass().getSimpleName()}"
                    }
                    errors.add(new JTEException(prefixMessage(msg)))
                }
            } else{
                errors.add(new JTEException(prefixMessage("missing required field '${requiredKey}'")))
            }
        }

        // validate optional keys
        optional.each{ optionalKey ->
            if(optionalKey in keys){
                keys -= optionalKey
                def actual = getProp(config, optionalKey)
                def expected = getProp(schema.fields?.optional, optionalKey)
                boolean keyHasError = validateType(logger, actual, expected)
                if (keyHasError){
                    String msg
                    if (expected instanceof java.util.regex.Pattern){
                        msg = "field ${optionalKey} must be a String matching ${expected} but is [${actual}]"
                    } else if (expected instanceof ArrayList){
                        msg = "field '${optionalKey}' must be one of ${expected} but is [${actual}]"
                    } else {
                        msg = "field '${optionalKey}' must be a ${expected.getSimpleName()} but is a ${actual.getClass().getSimpleName()}"
                    }
                    errors.add(new JTEException(prefixMessage(msg)))
                }
            }
        }

        // validate that there are no extraneous keys
        keys.each{ key ->
            errors.add(new JTEException(prefixMessage("field '${key}' is not used.")))
        }

        // if there are any errors, throw 'em
        if(errors.size()){
            throw errors
        }
    }

    LinkedHashMap parseSchema(String schema){
        PipelineConfigurationDsl dsl = new PipelineConfigurationDsl(flowOwner)
        PipelineConfigurationObject parsed = dsl.parse(schema)
        return parsed.getConfig()
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
                result << "${keyPrefix}${key}".toString()
            }
        }
        return result
    }

    /**
     * returns true if validateType finds an issue
     * @param logger Logger to print messages
     * @param actual the user defined value
     * @param expected the validation for the value
     * @return true if value fails validation
     */
    @SuppressWarnings(['MethodParameterTypeRequired', 'NoDef'])
    Boolean validateType(TemplateLogger logger, actual, expected){
        switch(expected){
            case [ boolean, Boolean ]:
                return !(actual.getClass() in [ boolean, Boolean ])
            case String:
                return !(actual.getClass() in [ String,  org.codehaus.groovy.runtime.GStringImpl ])
            case [ Integer, int]:
                return !(actual.getClass() in [ Integer, int ])
            case [ Double, BigDecimal, Float ]:
                return !(actual.getClass() in [ Double, BigDecimal, Float ])
            case Number:
                return !(actual instanceof Number)
            case { expected instanceof java.util.regex.Pattern }:
                if(!(actual.getClass() in [ String,  org.codehaus.groovy.runtime.GStringImpl ])){
                    return true
                }
                return !(actual.matches(expected))
            case [ List, ArrayList ]:
                return !(actual.getClass() in [List, ArrayList])
            case { expected instanceof ArrayList }:
                return !(actual in expected)
            default:
                logger.printWarning("Library Validator: Not sure how to handle value ${expected} with class ${expected.class}")
                return false
        }
    }

    private String prefixMessage(String message){
        return ([msgPrefix, message] - null).join(" ")
    }

}
