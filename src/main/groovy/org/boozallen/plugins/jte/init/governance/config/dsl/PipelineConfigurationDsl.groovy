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
package org.boozallen.plugins.jte.init.governance.config.dsl

import static PipelineConfigurationBuilder.*
import java.util.regex.Pattern
import org.apache.commons.lang.StringEscapeUtils
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Parses the pipeline configuration DSL into a {@link PipelineConfigurationObject}
 */
class PipelineConfigurationDsl {

    static class ConfigBlockMap extends LinkedHashMap{

        ConfigBlockMap(){}

        ConfigBlockMap( Map m){
            super(m)
        }

    }

    /*
        without this, something like:
        application_environments{
          dev
        }

        results in MissingPropertyException thrown for "dev"
     */
    static class DslBinding extends Binding {

        PipelineConfigurationObject pipelineConfig
        DslEnvVar env

        DslBinding(){ super() } // needed for unit tests

        DslBinding(PipelineConfigurationObject c, DslEnvVar d){
            this()
            this.env = d
            this.pipelineConfig = c
        }

        @Override Object getVariable(String property){
            switch (property){
                case PIPELINE_CONFIG_VAR: return pipelineConfig
                case ENV_VAR: return env
                default: return BuilderMethod.PROPERTY_MISSING
            }
        }
    }

    FlowExecutionOwner flowOwner

    PipelineConfigurationDsl(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
    }

    PipelineConfigurationObject parse(String scriptText){
        if(!flowOwner){
            throw new Exception("PipelineConfigurationDsl flowOwner not set. Can't determine current run.")
        }

        PipelineConfigurationObject pipelineConfig = new PipelineConfigurationObject(flowOwner)
        DslEnvVar env = new DslEnvVar(flowOwner)
        DslBinding ourBinding = new DslBinding( pipelineConfig, env )

        String processedScriptText = scriptText.replaceAll("@merge", "setMergeToTrue();")
                .replaceAll("@override", "setOverrideToTrue();")

        SecureGroovyScript script = new SecureGroovyScript("""
@groovy.transform.BaseScript org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationBuilder _
${processedScriptText}
""", true)
        script.configuringWithNonKeyItem()
        script.evaluate(this.getClass().getClassLoader(), ourBinding, flowOwner.listener)

        return pipelineConfig
    }

    String serialize(PipelineConfigurationObject configObj){
        Integer depth = 0
        ArrayList file = []
        ArrayList keys = []
        return printBlock(file, depth, configObj.getConfig(), keys, configObj).join("\n")
    }

    ArrayList printBlock(List file, Integer depth, Map block, ArrayList keys, PipelineConfigurationObject configObj){
        List appendedFile = file
        String tab = "    "
        block.each{ key, value ->
            String coordinate = keys.size() ? "${keys.join(".")}.${key}" : key
            String merge = (coordinate in configObj.merge) ? "@merge " : ""
            String override = (coordinate in configObj.override) ? "@override " : ""
            String nodeName = key.contains("-") ? "'${key}'" : key
            switch(value.getClass()){
                case ConfigBlockMap:
                    if (value == [:]){
                        appendedFile += "${tab * depth}${merge}${override}${nodeName}{}"
                    } else{
                        appendedFile += "${tab * depth}${merge}${override}${nodeName}{"
                        depth++
                        keys.push(key)
                        appendedFile = printBlock(appendedFile, depth, value, keys, configObj)
                        keys.pop()
                        depth--
                        appendedFile += "${tab * depth}}"
                    }
                    break
                case Map:
                    appendedFile += "${tab * depth}${merge}${override}${key} = ${value.inspect()}"
                    break
                case String:
                    appendedFile += "${tab * depth}${merge}${override}${key} = '${StringEscapeUtils.escapeJava(value)}'"
                    break
                case List:
                    appendedFile += "${tab * depth}${merge}${override}${key} = ${value.inspect()}"
                    break
                case Pattern:
                    appendedFile += "${tab * depth}${merge}${override}${key} = ~/${value}/"
                    break
                default:
                    appendedFile += "${tab * depth}${merge}${override}${key} = ${value}"
            }
        }
        return appendedFile
    }

}
