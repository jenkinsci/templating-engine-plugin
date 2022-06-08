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
package org.boozallen.plugins.jte.init.primitives.injectors;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Queue;
import hudson.model.Run;
import org.boozallen.plugins.jte.init.primitives.ReservedVariableName;
import org.boozallen.plugins.jte.init.primitives.hooks.HookContext;
import org.boozallen.plugins.jte.init.primitives.injectors.StageInjector.StageContext;
import org.boozallen.plugins.jte.init.primitives.injectors.StepContext;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The base script used during step execution
 *
 * context fields are set at runtime in StepWrapperCPS
 */
public abstract class StepWrapperScript extends CpsScript {

    private final Logger LOGGER = Logger.getLogger(StepWrapperScript.class.getName());

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
     * The step context
     */
    StepContext stepContext = new StepContext();

    /**
     * The FilePath within the build directory from which
     * resources can be fetched
     */
    private String resourcesPath;
    private String buildRootDir;

    public StepWrapperScript() throws IOException { super(); }

    @Override
    public Run<?,?> $buildNoException(){
        CpsFlowExecution execution = getExecution();
        if(execution == null){
            return null;
        }
        try{
            return $build();
        }catch(IOException x){
            LOGGER.log(Level.WARNING, null, x);
            return null;
        }
    }

    @Override
    public Run<?,?> $build() throws IOException {
        CpsFlowExecution execution = getExecution();
        if(execution == null){
            return null;
        }
        FlowExecutionOwner owner = getExecution().getOwner();
        Queue.Executable qe = owner.getExecutable();
        if (qe instanceof Run) {
            return (Run) qe;
        } else {
            return null;
        }
    }

    CpsFlowExecution getExecution(){
        CpsThread c = CpsThread.current();
        if (c == null){
            return null;
        }
        return c.getExecution();
    }


    public void setConfig(LinkedHashMap config){
        this.config = config;
    }
    public LinkedHashMap getConfig(){ return config; }

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class ConfigReservedVariable extends ReservedVariableName {
        public String getName(){ return "config"; }
        @Override public String getDescription(){
            return String.format("Variable name %s is reserved for steps to access their library configuration", getName());
        }
    }
  
    public void setHookContext(HookContext hookContext){
        this.hookContext = hookContext;
    }
    public HookContext getHookContext(){ return hookContext; }

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class HookContextReservedVariable extends ReservedVariableName {
        public String getName(){ return "hookContext"; }
        @Override public String getDescription(){
            return String.format("Variable name %s is reserved for steps to access their hook context", getName());
        }
    }

    public void setStageContext(StageContext stageContext){
        this.stageContext = stageContext;
    }
    public StageContext getStageContext(){ return stageContext; }

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class StageContextReservedVariable extends ReservedVariableName {
        public String getName(){ return "stageContext"; }
        @Override public String getDescription(){
            return String.format("Variable name %s is reserved for steps to access their stage context", getName());
        }
    }

    public void setStepContext(StepContext stepContext){
        this.stepContext = stepContext;
    }

    public StepContext getStepContext(){
        return this.stepContext;
    }

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class StepContextReservedVariable extends ReservedVariableName {
        public String getName(){ return "stepContext"; }
        @Override public String getDescription(){
            return String.format("Variable name %s is reserved for steps to access their step context", getName());
        }
    }

    public void setBuildRootDir(File rootDir){
        this.buildRootDir = rootDir.getPath();
    }

    public void setResourcesPath(String resourcesPath){
        this.resourcesPath = resourcesPath;
    }

    /**
     * Used within steps to access library resources
     *
     * @param path relative path within the resources directory to fetch
     * @return the resource file contents
     * @throws java.io.IOException throws this
     * @throws java.lang.InterruptedException throws this
     */
    public String resource(String path) throws IOException, InterruptedException {
        if (path.startsWith("/")){
            throw new AbortException("JTE: library step requested a resource that is not a relative path.");
        }
        File f = new File(buildRootDir);
        FilePath runRoot = new FilePath(f);
        FilePath resourcesBaseDir = runRoot.child(resourcesPath);
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

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class ResourceReservedVariable extends ReservedVariableName {
        public String getName(){ return "resource"; }
        @Override public String getDescription(){
            return String.format("Variable name %s is reserved for steps to access library resources", getName());
        }
    }

}
