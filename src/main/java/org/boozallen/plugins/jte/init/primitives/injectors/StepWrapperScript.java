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
import org.boozallen.plugins.jte.init.primitives.ReservedVariableName;
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
    public LinkedHashMap getConfig(){ return config; }

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class ConfigReservedVariable extends ReservedVariableName {
        public String getName(){ return "config"; }
        @Override public String getExceptionMessage(){
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
        @Override public String getExceptionMessage(){
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
        @Override public String getExceptionMessage(){
            return String.format("Variable name %s is reserved for steps to access their stage context", getName());
        }
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

    /**
     * reserves the config var from being overridden in the binding
     */
    @Extension public static class ResourceReservedVariable extends ReservedVariableName {
        public String getName(){ return "resource"; }
        @Override public String getExceptionMessage(){
            return String.format("Variable name %s is reserved for steps to access library resources", getName());
        }
    }
}
