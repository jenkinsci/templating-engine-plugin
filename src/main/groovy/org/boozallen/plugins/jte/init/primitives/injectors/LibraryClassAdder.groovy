import hudson.Extension
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveCollector
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob

import javax.annotation.CheckForNull
import java.lang.reflect.Field
import java.util.logging.Level
import java.util.logging.Logger

@Extension(ordinal=1.0D) // set ordinal > 0 so JTE comes before Declarative
class LibraryClassAdder extends GroovyShellDecorator {
    private static final Logger LOGGER = Logger.getLogger(LibraryClassAdder.name);

    @Override
    void configureShell(@CheckForNull CpsFlowExecution exec, GroovyShell shell) {
        if(!isFromJTE(exec)){
            return
        }

        FlowExecutionOwner flowOwner = exec.getOwner()
        TemplatePrimitiveCollector jte = flowOwner.run().getAction(TemplatePrimitiveCollector)
        if(jte == null){
            return
        }
        GroovyClassLoader classLoader = jte.classLoader
        if(classLoader == null){
            return
        }

        Field loaderF = GroovyShell.getDeclaredField("loader")
        loaderF.setAccessible(true)
        loaderF.set(shell, classLoader)

        // add loaded libraries `src` directories to the classloader
        File rootDir = flowOwner.getRootDir()
        File srcDir = new File(rootDir, "jte/src")
        if (srcDir.exists()){
            if(srcDir.isDirectory()) {
                shell.getClassLoader().addURL(srcDir.toURI().toURL())
            } else {
                LOGGER.log(Level.WARNING, "${srcDir.getPath()} is not a directory.")
            }
        }

    }

    /**
     * determines if the current pipeline is using JTE
     */
    boolean isFromJTE(CpsFlowExecution exec){
        if(!exec){
            return false // no execution defined yet, still initializing
        }
        WorkflowJob job = exec.getOwner().run().getParent()
        FlowDefinition definition = job.getDefinition()
        return (definition in TemplateFlowDefinition)
    }
}