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


import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.binding.TemplateException
import org.boozallen.plugins.jte.binding.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.utils.RunUtils
import spock.lang.*
import org.junit.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.boozallen.plugins.jte.binding.*

import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigException
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.jvnet.hudson.test.GroovyJenkinsRule

class ApplicationEnvironmentSpec extends Specification{

    TemplateBinding binding = new TemplateBinding()
    CpsScript script = GroovyMock(CpsScript)

    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared
    public ClassLoader classLoader = null

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

    void injectEnvironments(Map env_config){
        TemplateConfigObject config = new TemplateConfigObject(config: [
                application_environments: env_config
        ])
        ApplicationEnvironmentInjector.doInject(config, script)
    }


    def "Injector populates binding"(){
        when:
            def envName = "dev"
            injectEnvironments(["${envName}": [:]])
        then:
            assert binding.hasVariable(envName) 
            assert getApplicationEnvironmentClass().isInstance( binding.getVariable(envName) )
    }

    def "default short_name is environment key"(){
        when:
            def envName = "dev"
            injectEnvironments(["${envName}": [:]])
            def env = binding.getVariable(envName)
        then:
            getApplicationEnvironmentClass().isInstance( binding.getVariable(envName) )
            env.short_name == envName
    }

    def "set short_name"(){
        when:
            def envName = "dev"
            injectEnvironments([
                "${envName}": [
                    short_name: "develop"
                ]
            ])
            def env = binding.getVariable(envName)
        then:
            getApplicationEnvironmentClass().isInstance( binding.getVariable(envName) )
            env.short_name == "develop"
    }

    def "default long_name is environment key"(){
        when:
            def envName = "dev"
            injectEnvironments(["${envName}": [:]])
            def env = binding.getVariable(envName)
        then: 
            env.long_name == envName
    }

    def "set long_name"(){
        when:
            def envName = "dev"
            injectEnvironments([
                "${envName}": [
                    long_name: "develop"
                ]
            ])
            def env = binding.getVariable(envName)
        then: 
            assert env.long_name == "develop"
    }

    def "can set arbitrary additional key"(){
        when: 
            def envName = "dev" 
            injectEnvironments([
                "${envName}": [
                    random: 11
                ]
            ])
            def env = binding.getVariable(envName)
        then: 
            assert env.random == 11 
    }

    def "missing property returns null"(){
        when: 
            def envName = "dev" 
            injectEnvironments(["${envName}": [:]])
            def env = binding.getVariable(envName)
        then: 
            assert env.random == null 
    } 

    def "application environments are immutable"(){
        when: 
            def envName = "dev" 
            injectEnvironments(["${envName}": [:]])
            def env = binding.getVariable(envName)
            env.short_name = "something else" 
        then: 
            thrown(TemplateConfigException)
    }

    def "can set multiple environments"(){
        when: 
            injectEnvironments([
                dev: [:],
                test: [:]
            ])
        then: 
            assert binding.hasVariable("dev")
            assert binding.hasVariable("test")
    }

    def "fail on override during initialization"(){
        when: 
            injectEnvironments([dev: [:]])
            binding.setVariable("dev", "whatever")
        then:
        TemplateException ex = thrown()
            assert ex.message == "Application Environment dev already defined." 
    }

    def "fail on override post initialization"(){
        when: 
            injectEnvironments([dev: [:]])
            binding.lock()
            binding.setVariable("dev", "whatever")
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "Variable dev is reserved as an Application Environment." 
    }

    def "first environment's previous is null"(){
        when: 
            injectEnvironments([
                dev: [ long_name: "Development" ],
                test: [ long_name: "Test" ]
            ])
        then: 
            binding.getVariable("dev").previous == null 
    }
    
    def "second env's previous is correct"(){
        when: 
            injectEnvironments([
                dev: [ long_name: "Development" ],
                test: [ long_name: "Test" ]
            ])
        then: 
            binding.getVariable("test").previous == binding.getVariable("dev")
    }

    def "first env's next is correct"(){
        when: 
            injectEnvironments([
                dev: [ long_name: "Development" ],
                test: [ long_name: "Test" ]
            ])
        then: 
            binding.getVariable("dev").next == binding.getVariable("test")
    }

    def "when only one environment previous/next are null"(){
        when: 
            injectEnvironments([
                dev: [ long_name: "Development" ]
            ])
            def dev = binding.getVariable("dev")
        then: 
            dev.previous == null
            dev.next == null 
    }

    def "when >= 3 envs, middle envs previous and next are correct"(){
        when: 
            injectEnvironments([
                dev: [ long_name: "Development" ],
                test: [ long_name: "Test" ],
                prod: [ long_name: "Production" ]
            ])
            def test = binding.getVariable("test")
        then: 
            test.previous == binding.getVariable("dev")
            test.next == binding.getVariable("prod")
    }

    def "last environment's next is null"(){
        when: 
            injectEnvironments([
                    dev: [ long_name: "Development" ],
                    test: [ long_name: "Test" ],
                    prod: [ long_name: "Production" ]
                ])
            def prod = binding.getVariable("prod")
        then: 
            prod.next == null 
    }

    def "defining the previous configuration throws exception"(){
        when:
            injectEnvironments([
                dev: [ previous: "_" ]
            ])
        then: 
            thrown(TemplateConfigException)
    }

    def "defining the next configuration throws exception"(){
        when:
            injectEnvironments([
                dev: [ next: "_" ]
            ])
        then: 
            thrown(TemplateConfigException)
    }


    def getApplicationEnvironmentClass(){
        /* ApplicationEnvironmentInjector.primitiveClass */
        return TemplateScriptEngine.createShell().classLoader.loadClass("org.boozallen.plugins.jte.binding.injectors.ApplicationEnvironment")
    }
}