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

import org.boozallen.plugins.jte.init.dsl.TemplateConfigException
import org.boozallen.plugins.jte.init.primitives.TemplateException
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive

/*
    represents an immutable application environment.
*/
@SuppressWarnings(["PropertyName", "NoDef"])
class ApplicationEnvironment extends TemplatePrimitive implements Serializable{

    private static final long serialVersionUID = 1L
    String varName
    String short_name
    String long_name
    @SuppressWarnings("FieldTypeRequired") def config
    ApplicationEnvironment previous
    ApplicationEnvironment next

    ApplicationEnvironment(){}

    ApplicationEnvironment(String varName, Map config){
        this.varName = varName

        short_name = config.short_name ?: varName
        long_name = config.long_name ?: varName

        /*
            users cant define the previous or next properties. they'll
            just be ignored. so throw an exception if they try.
        */

        def context = config.subMap(["previous", "next"])
        if(context){
            throw new TemplateConfigException("""Error configuring ApplicationEnvironment ${varName}
            The previous and next configuration options are reserved and auto-populated.
            """.stripIndent())
        }

        this.config = config - config.subMap(["short_name", "long_name"])
        /*
            TODO:
                this makes it so that changing <inst>.config.whatever = <some value>
                will throw an UnsupportedOperationException.  Need to figure out how to
                throw TemplateConfigException instead for the sake of logging.
        */
        this.config = config.asImmutable()
    }

    Object getProperty(String name){
        def meta = ApplicationEnvironment.metaClass.getMetaProperty(name)
        return meta ? meta.getProperty(this) : config?."${name}"
    }

    @SuppressWarnings("UnusedMethodParameter")
    void setProperty(String name, Object value){
        throw new TemplateConfigException("Can't modify Application Environment '${long_name}'. Application Environments are immutable.")
    }

    void throwPreLockException(){
        throw new TemplateException ("Application Environment ${varName} already defined.")
    }

    void throwPostLockException(){
        throw new TemplateException ("Variable ${varName} is reserved as an Application Environment.")
    }

}
