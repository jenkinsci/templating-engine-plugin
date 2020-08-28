package org.boozallen.plugins.jte.init.primitives.injectors;

import hudson.AbortException;
import hudson.FilePath;
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext;
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * The base script used during step execution
 */
public abstract class StepWrapperScript extends CpsScript {

    /**
     * The library configuration
     */
    LinkedHashMap config = new LinkedHashMap();

    /**
     * The hook context information
     */
    HookContext hookContext = new HookContext();

    /**
     * The stage context
     */
    StageContext stageContext = new StageContext();

    /**
     * The FilePath within the build directory from which
     * resources can be fetched
     */
    private FilePath resourcesBaseDir;

    public StepWrapperScript() throws IOException { super(); }

    public void setConfig(LinkedHashMap config){
        this.config = config;
    }

    public LinkedHashMap getConfig(){
        return config;
    }

    public void setHookContext(HookContext hookContext){
        this.hookContext = hookContext;
    }

    public HookContext getHookContext(){
        return hookContext;
    }

    public void setStageContext(StageContext stageContext){
        this.stageContext = stageContext;
    }

    public StageContext getStageContext() {
        return stageContext;
    }

    public void setResourcesBaseDir(FilePath resourcesBaseDir) {
        this.resourcesBaseDir = resourcesBaseDir;
    }

    /**
     * Used within steps to access library resources
     *
     * @param path relative path within the resources directory to fetch
     * @return the resource file contents
     */
    public String resource(String path) throws IOException, InterruptedException {
        if (path.startsWith("/")){
            throw new AbortException("JTE: library step requested a resource that is not a relative path.");
        }
        FilePath resourceFile = resourcesBaseDir.child(path);
        if(!resourceFile.exists()){
            String oopsMsg = String.format("JTE: library step requested a resource '%s' that does not exist", path);
            throw new AbortException(oopsMsg);
        } else if(resourceFile.isDirectory()){
            String oopsMsg = String.format("JTE: library step requested a resource '%s' that is not a file.", path);
            throw new AbortException(oopsMsg);
        }
        return resourceFile.readToString();
    }
}
