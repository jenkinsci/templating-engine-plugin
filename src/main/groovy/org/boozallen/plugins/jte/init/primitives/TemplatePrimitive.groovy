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
package org.boozallen.plugins.jte.init.primitives

import com.cloudbees.groovy.cps.NonCPS
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.GlobalVariable

import javax.annotation.Nonnull

/**
 * Framework constructs that make templates easier to write.
 * Typically created by parsing the Pipeline Configuration.
 */
abstract class TemplatePrimitive extends GlobalVariable implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * The GlobalCollisionValidator will populate this list with all
     * TemplatePrimitives sharing the same name if there is
     * more than 1.
     */
    protected List<TemplatePrimitive> overloaded = []

    String name
    TemplatePrimitiveNamespace parent

    @SuppressWarnings("UnusedMethodParameter")
    @NonCPS
    Object getValue(@Nonnull CpsScript script, Boolean skipOverloaded = false) throws Exception {
        if(! skipOverloaded){
            isOverloaded()
        }
        return this
    }

    void setOverloaded(List<TemplatePrimitive> overloaded){
        this.overloaded = overloaded
    }

    String getParentChain(){
        List<String> parts = [ getName() ]
        TemplatePrimitiveNamespace parent = getParent()
        while(parent){
            parts.push(parent.getName())
            parent = parent.getParent()
        }
        parts.push(TemplatePrimitiveCollector.JTEVar.KEY)
        return parts.reverse().join(".")
    }

    protected void isOverloaded(){
        if(!overloaded.isEmpty()){
            TemplateLogger logger = TemplateLogger.createDuringRun()
            List<String> msg = [
                    "Attempted to access an overloaded primitive: ${getName()}",
                    "Please use fully qualified names to access the primitives.",
                    "options: "
            ]
            overloaded.each{ primitive ->
                msg.push("- ${primitive.getParentChain()}")
            }
            logger.printError(msg.join("\n"))
            throw new JTEException("Attempted to access an overloaded primitive: ${getName()}")
        }
    }

}
