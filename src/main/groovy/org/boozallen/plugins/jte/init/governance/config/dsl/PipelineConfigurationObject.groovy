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

import org.boozallen.plugins.jte.init.JteBlockWrapper
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * The parsed representation of a pipeline configuration
 * <p>
 *  Overrides the {@code plus} operator to implement the
 * aggregation logic when merging pipeline configurations.
 */
class PipelineConfigurationObject implements Serializable{

    private static final long serialVersionUID = 1L
    FlowExecutionOwner flowOwner
    LinkedHashMap config = [:]
    Set<String> merge = []
    Set<String> override = []
    boolean firstConfig = false

    PipelineConfigurationObject(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
    }

    JteBlockWrapper getJteBlockWrapper(){
        return (config.jte ?: [:]) as JteBlockWrapper
    }

    PipelineConfigurationObject plus(PipelineConfigurationObject child){
        /*
            If this is the first call to join, then there is no pre-existing
            configuration to merge.  set the current pipeline configuration to
            the child and return
        */
        if(firstConfig){
            printJoin(child, child, new PipelineConfigurationObject(flowOwner))
            return child
        }

        LinkedHashMap pipelineConfig
        PipelineConfigurationDsl dsl = new PipelineConfigurationDsl(flowOwner)
        PipelineConfigurationObject argCopy = dsl.parse(dsl.serialize(child))
        PipelineConfigurationObject prevCopy = dsl.parse(dsl.serialize(this))

        /*
            start out by wiping out any configurations that were
            already defined by the previous configuration.. bc governance
        */
        pipelineConfig = child.config + this.config

        /*
            if the current pipeline configuration allows children configurations
            to perform overrides for any block, then check the incoming pipeline
            configuration being joined to see if that block has been modified.

            If it has, overwrite the block with the child's.
        */
        this.override.each{ key ->
            if (get_prop(child.config, key) != null){
                clear_prop(pipelineConfig, key)
                if(get_prop(pipelineConfig, key) instanceof Map){
                    get_prop(pipelineConfig, key) << get_prop(child.config, key)
                } else {
                    set_prop(pipelineConfig, key, get_prop(child.config, key))
                }
            }
        }

        /*
            if the current pipeline configuration allows children configurations
            to perform merges for any block, then check the incoming pipeline
            configuration being joined to see if that block has been modified.

            If it has, add any new fields in the block but leave the already
            defined ones as is.
        */
        this.merge.each{ key ->
            if (get_prop(child.config, key) != null){
                get_prop(pipelineConfig, key) << (get_prop(child.config, key) + get_prop(pipelineConfig, key))
            }
        }

        // trim merge and overrides that don't apply
        // see: https://github.com/jenkinsci/templating-engine-plugin/issues/48
        LinkedHashMap r = getNested(pipelineConfig)
        child.merge = child.merge.findAll{ m ->
            r.keySet().collect{ key -> key.startsWith(m) }.contains(true)
        }
        child.override = child.override.findAll{ o ->
            r.keySet().collect{ key -> key.startsWith(o) }.contains(true)
        }

        child.setConfig(pipelineConfig)
        printJoin(child, argCopy, prevCopy)
        return child
    }

    @SuppressWarnings(['MethodReturnTypeRequired', 'NoDef'])
    private get_prop(LinkedHashMap o, String p){
        return p.tokenize('.').inject(o){ obj, prop ->
            obj?."$prop"
        }
    }

    private void clear_prop(LinkedHashMap o, String p){
        String lastToken
        if (p.tokenize('.')){
            lastToken = p.tokenize('.').last()
        } else if (InvokerHelper.getMetaClass(o).respondsTo(o, "clear", (Object[]) null)){
            o.clear()
        }
        p.tokenize('.').inject(o){ obj, prop ->
            if (prop == lastToken && InvokerHelper.getMetaClass(obj?."$prop").respondsTo(obj?."$prop", "clear", (Object[]) null)){
                obj?."$prop".clear()
            }
            obj?."$prop"
        }
    }

    @SuppressWarnings(['MethodParameterTypeRequired', 'ParameterReassignment', 'NoDef'])
    private void set_prop(LinkedHashMap o, String p, n){
        String lastToken
        if (p.tokenize('.')){
            lastToken = p.tokenize('.').last()
        } else if (InvokerHelper.getMetaClass(o).respondsTo(o, "clear", (Object[]) null)){
            o = n
        }
        p.tokenize('.').inject(o){ obj, prop ->
            if (prop == lastToken){
                obj?."$prop" = n
            }
            obj?."$prop"
        }
    }

    private LinkedHashMap getNested(LinkedHashMap map, List resultKeys = [], String keyPrefix = '') {
        LinkedHashMap ret = [:]
        map.each { key, value ->
            String pathKey = "${keyPrefix}${key}"
            if (value instanceof Map) {
                LinkedHashMap nestedMap = getNested(value, resultKeys, "${pathKey}.")
                if( nestedMap.isEmpty()){ // we are a leaf node and empty
                    ret[pathKey] = value
                    resultKeys << pathKey
                } else { // we are another map so add to existing map
                    ret = ret + nestedMap
                }
            } else {
                ret[pathKey] = value
                resultKeys << pathKey
            }
        }

        return ret
    }

    private void printJoin(PipelineConfigurationObject outcome, PipelineConfigurationObject incoming, PipelineConfigurationObject prev){
        // flatten each configuration for ease of delta analysis
        LinkedHashMap fOutcome = getNested(outcome.getConfig())
        LinkedHashMap fIncoming = getNested(incoming.getConfig())
        LinkedHashMap fPrevious = getNested(prev.getConfig())

        List<String> output = ['Pipeline Configuration Modifications']

        // Determine Configurations Added
        LinkedHashSet configurationsAdded = fOutcome.keySet() - fPrevious.keySet()
        if (configurationsAdded){
            output << "Configurations Added:"
            configurationsAdded.each{ key ->
                output << "- ${key} set to ${fOutcome[key]}"
            }
        } else{
            output << "Configurations Added: None"
        }

        // Determine Configurations Deleted
        LinkedHashSet configurationsDeleted = (fPrevious - fOutcome).keySet().findAll{ key -> !(key in fOutcome.keySet()) }
        if (configurationsDeleted){
            output << "Configurations Deleted:"
            configurationsDeleted.each{ key ->
                output << "- ${key}"
            }
        } else{
            output << "Configurations Deleted: None"
        }

        // Determine Configurations Changed
        LinkedHashSet configurationsChanged = (fOutcome - fPrevious).findAll{ entry -> entry.getKey() in fPrevious.keySet() }.keySet()
        if (configurationsChanged){
            output << "Configurations Changed:"
            configurationsChanged.each{ key  ->
                output << "- ${key} changed from ${fPrevious[key]} to ${fOutcome[key]}"
            }
        } else{
            output << "Configurations Changed: None"
        }

        // Determine Configurations Duplicated
        LinkedHashSet configurationsDuplicated = fPrevious.intersect(fIncoming).keySet()
        if (configurationsDuplicated){
            output << "Configurations Duplicated:"
            configurationsDuplicated.each{ key ->
                output << "- ${key}"
            }
        } else{
            output << "Configurations Duplicated: None"
        }

        // Determine Configurations Ignored
        LinkedHashSet configurationsIgnored = (fIncoming - fOutcome).keySet()
        if (configurationsIgnored){
            output << "Configurations Ignored:"
            configurationsIgnored.each{ key ->
                output << "- ${key}"
            }
        } else{
            output << "Configurations Ignored: None"
        }

        // Print Subsequent May Merge
        LinkedHashSet subsequentMayMerge = outcome.merge
        if(subsequentMayMerge){
            output << "Subsequent May Merge:"
            subsequentMayMerge.each{ key ->
                output << "- ${key}"
            }
        } else{
            output << "Subsequent May Merge: None"
        }

        // Print Subsequent May Override
        LinkedHashSet subsequentMayOverride = outcome.override
        if(subsequentMayOverride){
            output << "Subsequent May Override:"
            subsequentMayOverride.each{ key ->
                output << "- ${key}"
            }
        } else{
            output << "Subsequent May Override: None"
        }

        new TemplateLogger(flowOwner.getListener()).print(output.join("\n"))
    }

}
