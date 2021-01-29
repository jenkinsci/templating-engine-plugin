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

import org.boozallen.plugins.jte.init.PipelineDecorator
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper
import org.jenkinsci.plugins.workflow.cps.DSL
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.job.WorkflowRun

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
    private Boolean locked = false
    private final TemplateBindingRegistry registry = new TemplateBindingRegistry()
    private final Boolean permissiveInitialization

    TemplateBinding(FlowExecutionOwner owner, Boolean permissiveInitialization){
        this.permissiveInitialization = permissiveInitialization
        setVariable(STEPS, new DSL(owner))
        /**
         * for jte namespace, we need to bypass the exception throwing logic
         * that would be triggered by "jte" as a ReservedVariableName
         */
        variables.put(registry.getVariableName(), registry)
    }

    static TemplateBinding fetchDuringRun(){
        CpsThread thread = CpsThread.current()
        FlowExecution execution = thread?.getExecution()
        FlowExecutionOwner owner = execution?.getOwner()
        WorkflowRun run = owner.run()
        PipelineDecorator decorator = run.getAction(PipelineDecorator)
        return decorator.getBinding()
    }

    void lock(FlowExecutionOwner flowOwner){
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
        registry.printAllPrimitives(logger)
        locked = true
    }

    @Override @SuppressWarnings('NoDef')
    void setVariable(String name, Object value) {
        /**
         * if the variable being set is already taken by a TemplatePrimitive or marked
         * reserved by a ReservedVariableName, throw an exception
         */
        ReservedVariableName reservedVar = ReservedVariableName.byName(name)
        if (name in registry.getVariables() || reservedVar) {
            def collisionTarget = reservedVar ?: variables.get(name)
            if (!collisionTarget) {
                throw new JTEException("Something weird happened. Unable to determine source of binding collision.")
            }
            if (locked) {
                // during pipeline execution:
                //   always throw exceptions if overriding during pipeline execution
                //   i.e., a template or library inadvertently create a variable
                //   that collides
                collisionTarget.throwPostLockException()
            } else if (!permissiveInitialization || reservedVar) {
                // during initialization:
                // throw an exception if the initialization mode is strict
                // always throw exception if the collision target is a reserved variable
                collisionTarget.throwPreLockException()
            }
        }

        /**
         * add all template primitives to the namespace when
         * added to the binding
         */
        if (value in TemplatePrimitive){
            registry.add(value as TemplatePrimitive)
        }
        super.setVariable(name, value)
    }

    @Override
    Object getVariable(String name) {
        if (!variables) {
            throw new MissingPropertyException(name, this.getClass())
        }

        List<String> primitives = registry.getPrimitivesByName(name)
        if(primitives.size() >= 2 && locked){
            List<String> msg = [
                "Attempted to access an overloaded primitive:  ${name}",
                "Please use fully qualified names to access the primitives.",
                "options: "
            ]
            primitives.each{ p ->
                msg.push("  - ${p}")
            }
            TemplateLogger.createDuringRun().printError(msg.join("\n"))
            throw new JTEException("Attempted to access an overloaded primitive: ${name}")
        }

        Object result = variables.get(name)

        if (!result && !variables.containsKey(name)) {
            throw new MissingPropertyException(name, this.getClass())
        }

        /**
         * TemplatePrimitive's that implement getValue are able to specify an alternative value
         * when accessed in the binding. This is helpful when the users should interact with a
         * value encapsulated by the TemplatePrimitive, rather than the primitive object itself.
         */
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

    /**
     * retrieves all the primitives names/keys
     * @return
     */
    Set<String> getPrimitiveNames(){
        return this.registry.getVariables()
    }

}
