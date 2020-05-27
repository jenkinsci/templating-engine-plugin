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
package org.boozallen.plugins.jte.util

import org.jenkinsci.plugins.workflow.cps.CpsGroovyShellFactory

class TemplateScriptEngine implements Serializable{
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
}