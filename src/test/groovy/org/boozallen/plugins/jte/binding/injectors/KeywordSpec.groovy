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

package org.boozallen.plugins.jte.binding.injectors

import org.boozallen.plugins.jte.utils.RunUtils
import spock.lang.*
import org.junit.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.binding.*
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.jvnet.hudson.test.GroovyJenkinsRule

class KeywordSpec extends Specification{

    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared
    public ClassLoader classLoader = null

    TemplateBinding binding = new TemplateBinding() 
    CpsScript script = GroovyMock(CpsScript)

    def setupSpec(){
        classLoader = groovyJenkinsRule.jenkins.getPluginManager().uberClassLoader
    }

    def setup(){
        ClassLoader shellClassLoader = new groovy.lang.GroovyClassLoader(classLoader)

        GroovySpy(RunUtils.class, global:true)
        RunUtils.getClassLoader() >> { return shellClassLoader }

        GroovyShell shell = Spy(GroovyShell)
        shell.getClassLoader() >> { return shellClassLoader }

        GroovySpy(TemplateScriptEngine.class, global:true)
        TemplateScriptEngine.createShell() >> { return shell }

        _ * script.getBinding() >> {
            return binding 
        }
    }

    def getKeywordClass(){
        /* KeywordInjector.primitiveClass */
        return TemplateScriptEngine.createShell().classLoader.loadClass("org.boozallen.plugins.jte.binding.injectors.Keyword")
    }

    void injectKeywords(Map keywords){
        TemplateConfigObject config = new TemplateConfigObject(config: [
            keywords: keywords
        ])
        Class Keyword = getKeywordClass()
        KeywordInjector.doInject(config, script)
    }

    def "injector inserts keyword into binding"(){
        when: 
            injectKeywords([a: 1])
        then: 
            assert binding.hasVariable("a")
    }

    def "retrieving keyword from binding results in value"(){
        when: 
            injectKeywords([a: 1])
        then: 
            assert binding.getVariable("a") == 1 
    }

    def "inject multiple keywords"(){
        when: 
            injectKeywords([
                a: 1,
                b: 2
            ])
        then: 
            assert binding.hasVariable("a")
            assert binding.hasVariable("b")
    }

    def "override during initialization throws error"(){
        when:
            injectKeywords([a: 1])
            binding.setVariable("a", 2)
        then: 
            TemplateException ex = thrown()
            assert ex.message == "Keyword a already defined."
    }

    def "override post initialization throws error"(){
        when:
            injectKeywords([a: 1])
            binding.lock()
            binding.setVariable("a", 2)
        then: 
            TemplateException ex = thrown()
            assert ex.message == "Variable a is reserved as a template Keyword."
    }
}