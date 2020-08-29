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

import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.codehaus.groovy.runtime.InvokerHelper
import org.jenkinsci.plugins.workflow.cps.DSL
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Stores a run's primitives
 * <p>
 * A common TemplateBinding is used throughout the pipeline run. The same TemplateBinding instance is attached to the
 * pipeline template and each library step.
 * <p>
 * This binding implementation tracks the {@link org.boozallen.plugins.jte.init.primitives.TemplatePrimitive's} that
 * have been stored and prevents them from being overridden.
 */
class TemplateBinding extends Binding implements Serializable{

    private static final long serialVersionUID = 1L
    private static final String STEPS = "steps"
    @SuppressWarnings('PrivateFieldCouldBeFinal') // could be modified during pipeline execution
    private Set<String> registry = []
    private Boolean locked = false

    TemplateBinding(FlowExecutionOwner owner){
        setVariable(STEPS, new DSL(owner))
    }

    void lock(){
        locked = true
    }

    @Override @SuppressWarnings('NoDef')
    void setVariable(String name, Object value) {
        if (name in registry || ReservedVariableName.byName(name)){
            def thrower = ReservedVariableName.byName(name) ?: variables.get(name)
            if(!thrower){
                throw new Exception("Something weird happened. Unable to determine source of binding collision.")
            }
            if (locked){
                thrower.throwPostLockException()
            } else{
                thrower.throwPreLockException()
            }
        }
        if (value in TemplatePrimitive){
            registry << name
        }
        super.setVariable(name, value)
    }

    @Override
    Object getVariable(String name) {
        if (!variables) {
            throw new MissingPropertyException(name, this.getClass())
        }
        Object result = variables.get(name)

        if (!result && !variables.containsKey(name)) {
            throw new MissingPropertyException(name, this.getClass())
        }

        if (result in TemplatePrimitive && InvokerHelper.getMetaClass(result).respondsTo(result, "getValue", (Object[]) null)){
            result = result.getValue()
        }
        return result
    }

    Boolean hasStep(String stepName){
        if (hasVariable(stepName)){
            Class stepClass = StepWrapperFactory.getPrimitiveClass()
            return getVariable(stepName).getClass().getName() == stepClass.getName()
        }
        return false
    }

    @SuppressWarnings(['NoDef', 'MethodReturnTypeRequired'])
    def getStep(String stepName){
        if (hasStep(stepName)){
            return getVariable(stepName)
        }
        throw new TemplateException("No step ${stepName} has been loaded")
    }

}
