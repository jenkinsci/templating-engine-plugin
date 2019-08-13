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

package org.boozallen.plugins.jte.binding.injectors

import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.binding.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 

/*
    represents an immutable application environment. 
*/
class ApplicationEnvironment extends TemplatePrimitive implements Serializable{
    String var_name
    String short_name
    String long_name
    final def config
    ApplicationEnvironment previous 
    ApplicationEnvironment next 

    
    ApplicationEnvironment(){}

    ApplicationEnvironment(String var_name, Map _config){ 
        this.var_name = var_name

        short_name = _config.short_name ?: var_name 
        long_name = _config.long_name ?: var_name 

        /*
            users cant define the previous or next properties. they'll
            just be ignored. so throw an exception if they try. 
        */

        def context = _config.subMap(["previous", "next"])
        if(context){
            throw new TemplateConfigException("""Error configuring ApplicationEnvironment ${var_name}
            The previous and next configuration options are reserved and auto-populated. 
            """.stripIndent())
        }
        
        config = _config - _config.subMap(["short_name", "long_name"])
        /*
            TODO: 
                this makes it so that changing <inst>.config.whatever = <some value> 
                will throw an UnsupportOperationException.  Need to figure out how to 
                throw TemplateConfigException instead for the sake of logging.
        */
        config = config.asImmutable()
    }
    
    Object getProperty(String name){
        def meta = ApplicationEnvironment.metaClass.getMetaProperty(name)
        if (meta) {
            meta.getProperty(this)
        } else {
            if (config.containsKey(name)){ return config.get(name) }
            else { return null }
        }
    }

    void setProperty(String name, Object value){
        throw new TemplateConfigException("Can't modify Application Environment '${long_name}'. Application Environments are immutable.")
    }

    void throwPreLockException(){
        throw new TemplateException ("Application Environment ${var_name} already defined.")
    }

    void throwPostLockException(){
        throw new TemplateException ("Variable ${var_name} is reserved as an Application Environment.")
    }

}