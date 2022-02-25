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

import org.boozallen.plugins.jte.init.governance.config.dsl.TemplateConfigException
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive

/**
 * JTE primitive representing an application environment to capture environmental context
 */
@SuppressWarnings(["PropertyName", "FieldTypeRequired", "NoDef"])
class ApplicationEnvironment extends TemplatePrimitive implements Serializable{

    private static final long serialVersionUID = 1L
    String name
    String short_name
    String long_name
    def config
    ApplicationEnvironment previous
    ApplicationEnvironment next

    ApplicationEnvironment(){}

    ApplicationEnvironment(String name, Map config){
        this.name = name

        short_name = config.short_name ?: name
        long_name = config.long_name ?: name

        /*
            users cant define the previous or next properties. they'll
            just be ignored. so throw an exception if they try.
        */

        def context = config.subMap(["previous", "next"])
        if(context){
            throw new TemplateConfigException("""Error configuring ApplicationEnvironment ${name}
            The previous and next configuration options are reserved and auto-populated.
            """.stripIndent())
        }

        config = config - config.subMap(["short_name", "long_name"])
        this.config = config.asImmutable()
    }

    @Override String getName(){ return name }

    Object getProperty(String property){
        // first check user-defined configuration
        if (config.containsKey(property)){
            return config?."${property}"
        }

        // then check this object itself
        def meta = ApplicationEnvironment.metaClass.getMetaProperty(property)
        if (meta){
            return meta.getProperty(this)
        }

        // otherwise return null if property is missing
        return null
    }

    @SuppressWarnings("UnusedMethodParameter")
    void setProperty(String name, Object value){
        throw new TemplateConfigException("Can't modify Application Environment '${long_name}'. Application Environments are immutable.")
    }

    @Override String toString(){
        return "Application Environment '${name}'"
    }

}
