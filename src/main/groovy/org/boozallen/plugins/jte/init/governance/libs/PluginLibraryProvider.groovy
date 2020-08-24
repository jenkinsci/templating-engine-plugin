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
package org.boozallen.plugins.jte.init.governance.libs

import hudson.Extension
import hudson.FilePath
import hudson.model.Descriptor
import hudson.model.DescriptorVisibilityFilter
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.primitives.injectors.StepWrapperFactory
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor

import java.nio.charset.StandardCharsets
import java.security.CodeSource
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class PluginLibraryProvider extends LibraryProvider{

    private final LibraryProvidingPlugin plugin
    private final HashMap libraries = [:]

    @DataBoundConstructor PluginLibraryProvider(LibraryProvidingPlugin plugin){
        this.plugin = plugin
        initialize()
    }

    /*
        analyzes the contributing plugin's jar file to find libraries
        housed in the resources directory.

        populates this.libraries with data from parsing
    */
    void initialize(){
        // initialize libraries
        CodeSource src = plugin.getClass().getProtectionDomain().getCodeSource()
        URL jar = src.getLocation()
        ZipFile zipFile = new ZipFile(new File(jar.toURI()))
        ZipInputStream zipStream = new ZipInputStream(jar.openStream())
        ZipEntry zipEntry
        while( (zipEntry = zipStream.getNextEntry()) != null ){
            String path = zipEntry.getName()
            ArrayList parts = path.split("/")
            if(path.startsWith("libraries/") && parts.size() >= 3){
                String libName = parts[1]
                // create new library entry if we haven't seen this before
                if(!libraries.containsKey(libName)){
                    libraries[libName] = [
                        steps: [:],
                        resources: [:],
                        config: null
                    ]
                }

                String thing = parts[2]
                if(thing == CONFIG_FILE){
                    libraries[libName] = getFileContents(zipFile, zipEntry)
                } else if (thing in [ "steps", "resources"]){
                    String relativePath = path - "libraries/${libName}/"
                    libraries[libName][thing][relativePath] = getFileContents(zipFile, zipEntry)
                }
             }
        }
    }

    String getFileContents(ZipFile z, ZipEntry e){
        InputStream stream = z.getInputStream(e)
        ArrayList lines = []
        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            String line
            while((line = bufferedReader.readLine()) != null){
                lines << line
            }
        } catch(ignored){}

        return lines.join("\n")
    }

    @Override
    Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName){
        return libName in libraries.keySet()
    }

    @Override
    List loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig){
        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        ArrayList msg = [
            "Loading Library ${libName}",
            "-- plugin: ${getPluginDisplayName() ?: "can't determine plugin"}"
        ]
        logger.print(msg.join("\n"))

        // do library configuration
        ArrayList libConfigErrors = []
        if(libraries[libName]?.config){
            libConfigErrors = doLibraryConfigValidation(flowOwner, libraries[libName].config, libConfig)
            if(libConfigErrors){
                return [ "${libName}:" ] + libConfigErrors.collect{ error -> " - ${error}" }
            }
        } else{
            logger.printWarning("Library ${libName} does not have a configuration file.")
        }

        // copy the library contents into the build dir
        FilePath buildRootDir = new FilePath(flowOwner.getRootDir())
        FilePath rootDir = buildRootDir.child("jte/${libName}")
        rootDir.mkdirs()
        (libraries[libName].steps + libraries[libName].resources).each { filePath, fileContents ->
            FilePath file = rootDir.child(filePath)
            file.write(fileContents, "UTF-8")
        }

        // create StepWrappers and inject
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        libraries[libName].steps.each{ stepPath, stepContents ->
            String stepName = stepPath.split("/").last() - ".groovy"
            binding.setVariable(stepName, stepFactory.createFromFilePath(
                rootDir.child(stepPath),
                binding,
                libName,
                libConfig
            ))
        }
        return libConfigErrors
    }

    /*
        returns null if for some reason plugin descriptor can't be found
        -- this shouldn't technically be possible.
    */
    String getPluginDisplayName(){
        List<LibraryProvidingPlugin> plugins = DescriptorImpl.getLibraryProvidingPlugins()
        Descriptor pluginDescriptor = Descriptor.findByDescribableClassName(plugins, plugin.getClass().getName())
        return pluginDescriptor?.getDisplayName()
    }

    @Extension
    static class DescriptorImpl extends LibraryProvider.LibraryProviderDescriptor{

        static List<LibraryProvidingPlugin> getLibraryProvidingPlugins(){
            return Jenkins.get().getExtensionList(LibraryProvidingPlugin.LibraryProvidingPluginDescriptor)
        }

        String getDisplayName(){
            return "From a Library Providing Plugin"
        }
    }

    /*
        hide this plugin as an option if there aren't any plugin providing libraries
        installed on the jenkins instance
    */
    @Extension
    static class FilterImpl extends DescriptorVisibilityFilter {
        @Override
        boolean filter(Object context, Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl){
                return !DescriptorImpl.getLibraryProvidingPlugins().isEmpty()
            }
            return true
        }
    }

}
