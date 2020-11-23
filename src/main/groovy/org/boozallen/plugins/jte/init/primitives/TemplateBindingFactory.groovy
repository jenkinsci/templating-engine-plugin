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

import hudson.ExtensionList
import org.boozallen.plugins.jte.init.PipelineDecorator.JteBlockWrapper
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.reflection.CachedMethod
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator
import org.jgrapht.alg.cycle.CycleDetector

import java.lang.reflect.Method

/**
 * creates, populates, and returns the run's {@link TemplateBinding}
 *
 */
class TemplateBindingFactory {

    static TemplateBinding create(FlowExecutionOwner flowOwner, PipelineConfigurationObject config){
        invoke("validateConfiguration", flowOwner, config)
        JteBlockWrapper jte = config.jteBlockWrapper
        TemplateBinding templateBinding = new TemplateBinding(flowOwner, jte.permissive_initialization)
        invoke("injectPrimitives", flowOwner, config, templateBinding)
        invoke("validateBinding", flowOwner, config, templateBinding)
        templateBinding.lock(flowOwner)
        return templateBinding
    }

    private static void invoke(String phase, Object... args){
        List<Class<? extends TemplatePrimitiveInjector>> failedInjectors = []
        Graph<Class<? extends TemplatePrimitiveInjector>, DefaultEdge> graph = createGraph(phase, args)
        AggregateException errors = new AggregateException()
        new TopologicalOrderIterator<>(graph).each{ injectorClazz ->
            TemplatePrimitiveInjector injector = injectorClazz.getDeclaredConstructor().newInstance()
            try{
                // check if a dependent injector has failed, if so, don't execute
                if(!(getPrerequisites(injector, phase, args).intersect(failedInjectors))){
                    injector.invokeMethod(phase, args)
                }
            } catch(any){
                TemplateLogger logger = new TemplateLogger(args[0].getListener())
                String msg = [ any.getMessage(), any.getStackTrace()*.toString() ].flatten().join("\n")
                logger.printError(msg)
                errors.add(any)
                failedInjectors << injectorClazz
            }
        }
        if(errors.size()) { // this phase failed throw an exception
            throw errors
        }
    }

    private static List<Class<? extends TemplatePrimitiveInjector>> getPrerequisites(TemplatePrimitiveInjector injector, String name, Object... args){
        List<TemplatePrimitiveInjector> prereqs = []
        MetaMethod metaMethod = injector.metaClass.pickMethod(name, args*.class as Class[])
        if(metaMethod instanceof CachedMethod) {
            Method method = metaMethod.getCachedMethod()
            RunAfter annotation = method.getAnnotation(RunAfter)
            if (annotation) {
                prereqs = [annotation.value()].flatten() as List<Class<? extends TemplatePrimitiveInjector>>
            }
        }
        return prereqs
    }

    /**
     * Builds a Directed Acyclic Graph representing the order in which the {@link TemplatePrimitiveInjector's} should
     * be invoked.
     * <p>
     *
     *
     * @param name the phase of binding population to a graph of
     * @param args
     * @return
     */
    private static Graph<Class<? extends TemplatePrimitiveInjector>, DefaultEdge> createGraph(String name, Object... args){
        Graph<Class<? extends TemplatePrimitiveInjector>, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge)
        ExtensionList<TemplatePrimitiveInjector> injectors = TemplatePrimitiveInjector.all()
        // add each injector as a node in the graph
        injectors.each{ injector -> graph.addVertex(injector.class) }

        // for each injector, connect edges
        injectors.each{ injector ->
            List<Class<? extends TemplatePrimitiveInjector>> prereqs = getPrerequisites(injector, name, args)
            prereqs.each{ req ->
                graph.addEdge(req, injector.class)
            }
        }

        // check for infinite loops
        CycleDetector detector = new CycleDetector(graph)
        Set<TemplatePrimitiveInjector> cycles = detector.findCycles()
        if(cycles){
            throw new JTEException("There are cyclic dependencies preventing initialization: ${cycles}")
        }

        return graph
    }

}
