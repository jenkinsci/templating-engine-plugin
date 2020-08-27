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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringEscapeUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.groovy.sandbox.SandboxTransformer

/**
 * Parses the pipeline configuration DSL into a {@link PipelineConfigurationObject}
 */
class PipelineConfigurationDsl {

    FlowExecutionOwner flowOwner

    PipelineConfigurationDsl(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
    }

    PipelineConfigurationObject parse(String scriptText){
        if(!flowOwner){
            throw new Exception("PipelineConfigurationDsl flowOwner not set. Can't determine current run.")
        }

        PipelineConfigurationObject pipelineConfig = new PipelineConfigurationObject(flowOwner)
        EnvActionImpl env = EnvActionImpl.forRun(flowOwner.run())
        Binding ourBinding = new Binding(
                pipelineConfig: pipelineConfig,
                env: env
        )

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.addCompilationCustomizers(new SandboxTransformer())
        cc.scriptBaseClass = PipelineConfigurationBuilder.name

        GroovyShell sh = new GroovyShell(this.getClass().getClassLoader(), ourBinding, cc)
        String processedScriptText = scriptText.replaceAll("@merge", "setMergeToTrue();")
                                               .replaceAll("@override", "setOverrideToTrue();")
        Script script = sh.parse(processedScriptText)

        DslSandbox sandbox = new DslSandbox(script, env)
        sandbox.register()
        try {
            script.run()
        } finally {
            sandbox.unregister()
        }

        return pipelineConfig
    }

    String serialize(PipelineConfigurationObject configObj){
        Map config = new JsonSlurper().parseText(JsonOutput.toJson(configObj.getConfig()))

        Integer depth = 0
        ArrayList file = []
        ArrayList keys = []
        return printBlock(file, depth, config, keys, configObj).join("\n")
    }

    ArrayList printBlock(List file, Integer depth, Map block, ArrayList keys, PipelineConfigurationObject configObj){
        List appendedFile = file
        String tab = "    "
        block.each{ key, value ->
            String coordinate = keys.size() ? "${keys.join(".")}.${key}" : key
            String merge = (coordinate in configObj.merge) ? "@merge " : ""
            String override = (coordinate in configObj.override) ? "@override " : ""
            switch(value.getClass()){
                case Map:
                    String nodeName = key.contains("-") ? "'${key}'" : key
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
                case String:
                    appendedFile += "${tab * depth}${merge}${override}${key} = '${StringEscapeUtils.escapeJava(value)}'"
                    break
                case List:
                    appendedFile += "${tab * depth}${merge}${override}${key} = ${value.inspect()}"
                    break
                default:
                    appendedFile += "${tab * depth}${merge}${override}${key} = ${value}"
            }
        }
        return appendedFile
    }

}
