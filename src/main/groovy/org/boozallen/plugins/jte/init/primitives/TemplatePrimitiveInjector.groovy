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
import hudson.ExtensionPoint
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.AggregateException
import org.boozallen.plugins.jte.util.JTEException
import org.codehaus.groovy.reflection.CachedMethod
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.CpsGroovyShellFactory
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jgrapht.Graph
import org.jgrapht.alg.cycle.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator

import java.lang.reflect.Method

/**
 * Extension to hook into the initialization process to parse the pipeline configuration,
 * create TemplatePrimitives, and validate them after creation
 */
@SuppressWarnings(['EmptyMethodInAbstractClass', 'UnusedMethodParameter'])
abstract class TemplatePrimitiveInjector implements ExtensionPoint{

    /**
     * fetches all registered TemplatePrimitiveInjectors
     *
     * @return list of TemplatePrimitiveInjectors
     */
    static ExtensionList<TemplatePrimitiveInjector> all(){
        return Jenkins.get().getExtensionList(TemplatePrimitiveInjector)
    }

    /**
     * Used to compile pipeline primitives using the CPS transformer.
     * @param classText the source code text
     * @return the Class represented by classText
     */
    static Class parseClass(String classText){
        GroovyShell shell = new CpsGroovyShellFactory(null).forTrusted().build()
        GroovyClassLoader classLoader = shell.getClassLoader()
        GroovyClassLoader tempLoader = new GroovyClassLoader(classLoader)
        /*
            Creating a new, short-lived class loader that inherits the
            compiler configuration of the pipeline's is the easiest
            way to parse a file and see if the class has been loaded
            before
        */
        Class clazz = tempLoader.parseClass(classText)
        Class returnClass = clazz
        if(classLoader.getClassCacheEntry(clazz.getName())){
            // class has been loaded before. fetch and return
            returnClass = classLoader.loadClass(clazz.getName())
        } else {
            // class has not be parsed before, add to the runs class loader
            classLoader.setClassCacheEntry(returnClass)
        }
        return returnClass
    }

    /**
     * Triggers the 3-phase initialization process by iterating over all registered injectors
     *
     * @param flowOwner the run's FlowExecutionOwner
     * @param config the aggregated pipeline configuration
     */
    static void orchestrate(CpsFlowExecution exec, PipelineConfigurationObject config){
        invoke("validateConfiguration", exec, config)
        FlowExecutionOwner flowOwner = exec.getOwner()
        WorkflowRun run = flowOwner.run()
        TemplatePrimitiveCollector collector = new TemplatePrimitiveCollector()
        run.addOrReplaceAction(collector) // may be used by one of the injectors
        invoke(run, collector, "injectPrimitives", exec, config)
        invoke("validatePrimitives", exec, config, collector)
        run.addOrReplaceAction(collector)
    }

    /**
     * Used to validate the pipeline configuration is structurally correct
     * @param exec the current CpsFlowExecution
     * @param config the aggregated pipeline configuration
     */
    void validateConfiguration(CpsFlowExecution exec, PipelineConfigurationObject config){}

    /**
     * parse the aggregated pipeline configuration to instantiate a {@link TemplatePrimitive} and store it
     * in a TemplatePrimitiveNamespace
     *
     * @param flowOwner the run's flowOwner
     * @param config the aggregated pipeline configuration
     */
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config){ return null }

    /**
     * A second pass allowing the different injector's to inspect what TemplatePrimitives
     * have been created and respond accordingly
     *
     * @param flowOwner the run's flowOwner
     * @param config the aggregated pipeline configuration
     */
    void validatePrimitives(CpsFlowExecution exec, PipelineConfigurationObject config, TemplatePrimitiveCollector collector){}

    TemplatePrimitiveCollector getPrimitiveCollector(CpsFlowExecution exec){
        WorkflowRun run = exec.getOwner().run()
        if(!run){
            throw new JTEException("Invalid Context. Cannot determine run.")
        }
        TemplatePrimitiveCollector primitiveCollector = run.getAction(TemplatePrimitiveCollector)
        if(primitiveCollector == null){
            primitiveCollector = new TemplatePrimitiveCollector()
        }
        return primitiveCollector
    }

    /**
     * Invokes a specific phase of initialization
     *
     * @param phase the phase to invoke
     * @param args the args to pass to the phase
     */
    private static void invoke(WorkflowRun run = null, TemplatePrimitiveCollector collector = null, String phase, Object... args){
        List<Class<? extends TemplatePrimitiveInjector>> failedInjectors = []
        Graph<Class<? extends TemplatePrimitiveInjector>, DefaultEdge> graph = createGraph(phase, args)
        AggregateException errors = new AggregateException()
        new TopologicalOrderIterator<>(graph).each{ injectorClazz ->
            TemplatePrimitiveInjector injector = injectorClazz.getDeclaredConstructor().newInstance()
            try{
                // check if a dependent injector has failed, if so, don't execute
                if(!(getPrerequisites(injector, phase, args).intersect(failedInjectors))){
                    TemplatePrimitiveNamespace namespace = injector.invokeMethod(phase, args)
                    if(namespace){
                        collector.addNamespace(namespace)
                        run.addOrReplaceAction(collector)
                    }
                }
            } catch(any){
                errors.add(any)
                failedInjectors << injectorClazz
            }
        }
        if(errors.size()) { // this phase failed throw an exception
            CpsFlowExecution exec = args[0]
            errors.printToConsole(exec)
            errors.setMessage("JTE Pipeline Initialization failed during ${phase}")
            throw errors
        }
    }

    /**
     * Determines which injectors must run prior to a given Injector
     *
     * @param injector the injector to find dependencies for
     * @param name the phase of the injector initialization process to find dependencies for
     * @param args the arguments passed to the initialization phase method
     * @return the list of prerequisite dependencies
     */
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
