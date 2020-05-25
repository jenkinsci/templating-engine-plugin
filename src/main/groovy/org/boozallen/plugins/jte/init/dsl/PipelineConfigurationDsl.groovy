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
package org.boozallen.plugins.jte.init.dsl

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringEscapeUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.groovy.sandbox.SandboxTransformer

class PipelineConfigurationDsl {

  FlowExecutionOwner flowOwner

  PipelineConfigurationDsl(FlowExecutionOwner flowOwner){
    this.flowOwner = flowOwner
  }

  PipelineConfigurationObject parse(String script_text){

    if(!flowOwner){
      throw new Exception("PipelineConfigurationDsl flowOwner not set. Can't determine current run.")
    }

    PipelineConfigurationObject pipelineConfig = new PipelineConfigurationObject(flowOwner)
    EnvActionImpl env = EnvActionImpl.forRun(flowOwner.run())
    Binding our_binding = new Binding(
      pipelineConfig: pipelineConfig,
      env: env
    )

    CompilerConfiguration cc = new CompilerConfiguration()
    cc.addCompilationCustomizers(new SandboxTransformer())
    cc.scriptBaseClass = PipelineConfigurationBuilder.class.name

    GroovyShell sh = new GroovyShell(this.getClass().getClassLoader(), our_binding, cc)
    script_text = script_text.replaceAll("@merge", "builderMerge();")
    script_text = script_text.replaceAll("@override", "builderOverride();")
    Script script = sh.parse(script_text)

    DslSandbox sandbox = new DslSandbox(script, env)
    sandbox.register()
    try {
      script.run()
    }finally {
      sandbox.unregister()
    }

    return pipelineConfig
  }


  String serialize(PipelineConfigurationObject configObj){
    Map config = new JsonSlurper().parseText(JsonOutput.toJson(configObj.getConfig()))

    def depth = 0
    ArrayList file = [] 
    ArrayList keys = []
    return printBlock(file, depth, config, keys, configObj).join("\n")
  }

  ArrayList printBlock(ArrayList file, depth, Map block, ArrayList keys, PipelineConfigurationObject configObj){
    String tab = "    "
    block.each{ key, value -> 
      String coordinate = keys.size() ? "${keys.join(".")}.${key}" : key 
      String merge = (coordinate in configObj.merge) ? "@merge " : "" 
      String override = (coordinate in configObj.override) ? "@override " : "" 
      if(value instanceof Map){
        String nodeName = key.contains("-") ? "'${key}'" : key
        if (value == [:]){
          file += "${tab*depth}${merge}${override}${nodeName}{}"
        }else{
          file += "${tab*depth}${merge}${override}${nodeName}{"
          depth++
          keys.push(key)
          file = printBlock(file, depth, value, keys, configObj)
          keys.pop()
          depth-- 
          file += "${tab*depth}}"
        }
      }else{
        if (value instanceof String){
          file += "${tab*depth}${merge}${override}${key} = '${StringEscapeUtils.escapeJava(value)}'"
        } else if (value instanceof ArrayList){
          file += "${tab*depth}${merge}${override}${key} = ${value.inspect()}" 
        }else{
          file += "${tab*depth}${merge}${override}${key} = ${value}" 
        }
      }
    }
    return file
  }
}
