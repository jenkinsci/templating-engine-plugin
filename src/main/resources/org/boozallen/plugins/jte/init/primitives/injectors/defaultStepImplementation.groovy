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
package org.boozallen.plugins.jte.init.primitives.injectors

@SuppressWarnings("NoDef")
void call(){
    String errorMsg = """
     step definition specification:

     steps{
       unit_test{
         stage = "Unit Test"            // optional. display name for step. defaults to ${config.name}
         image = "maven"                // required. docker image to use for testing
         command = "mvn clean verify"   // either command or script
         script = ./tests/unit_test.sh  // not both. one required.
         stash{                         // stash is optional
           name = "test-results"        // stash name required if stash is configured
           includes = "./target"        // optional. defaults to everything in pwd
           excludes = "./src"           // optional. defaults to nothing
           useDefaultExcludes = false   // optional. defaults to true
           allowEmpty = true            // optional. defaults to false
         }
       }
     }
     """

    stage(config.stage ?: config.name){
        // get docker image for step
        def img = config.image ?: {
            error "Image not defined for default step implementation ${config.name}. \n ${errorMsg}"
        }()

        // execute step
        docker.image(img).inside{
            /*
                this part of the framework needs some thought..
                how to pass the workspace around so libraries can access files
                from scm without all having to do a clone.

                for now - we stash the workspace at the beginning of the build
                after doing a `checkout scm`.
            */
            try{
                unstash "workspace"
            } catch(ignore){}

            // validate only one of command or script is set
            if (!config.subMap(["command", "script"]).size() == 1) {
                error error_msg
            }

            // get command to run inside image
            String scriptText
            if (config.command){
                scriptText = config.command
            }

            if (config.script){
                if (fileExists(config.script)){
                    scriptText = readFile config.script
                } else{
                    error "Script ${config.script} not found"
                }
            }

            sh scriptText

            // stash results if configured
            def s = config.stash
            if (s){
                // validate stash configuration
                String n = s.name ?: { error "Step ${config.name} stash name not configured: \n ${errorMsg}" }.call()
                String i = s.includes ?: "**"
                String e = s.excludes ?: " "
                boolean d = s.useDefaultExcludes ?: true
                boolean p = s.allowEmpty ?: false

                stash name: n,
                        includes: i,
                        excludes: e,
                        useDefaultExcludes: d,
                        allowEmpty: p
            }
        }
    }
}
