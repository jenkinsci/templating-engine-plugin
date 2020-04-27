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

package org.boozallen.plugins.jte.config


import org.boozallen.plugins.jte.console.TemplateLogger
import org.junit.ClassRule
import org.jvnet.hudson.test.GroovyJenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.IgnoreRest
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl 

class PipelineConfigSpec extends Specification {
    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()
    public PrintStream logger = Mock()
    public logs = [] 

    def setup(){
        // mock run
        GroovySpy(TemplateConfigDsl, global:true)
        _ * TemplateConfigDsl.getEnvironment() >> GroovyMock(EnvActionImpl)

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_, _) >> { s, c -> logs.push(s) }
    }

    def 'first join == aggregated config'(){
        setup:
        TemplateConfigObject config = new TemplateConfigObject(
            config: [ someField: true ],
            merge: [],
            override: []
        )
        
        when: 
        PipelineConfig p = new PipelineConfig()
        p.join(config)

        then:
        p.getConfig() == config 
    }

    def 'Join empty TemplateConfigObjects to PipelineConfig'(){
        when: 
        TemplateConfigObject aggregate = aggregate("", "")

        then:
        aggregate.config == [:]
        aggregate.merge.isEmpty()
        aggregate.override.isEmpty()
    }

    def 'Keys 2 tiers Configuration'(){
        when: 
        TemplateConfigObject aggregate = aggregate("a=1", "b=2")
        
        then:
        aggregate.getConfig() == [a: 1, b: 2]
        aggregate.merge.isEmpty()
        aggregate.override.isEmpty()
    }

    def 'Keys with 2 tier override Configuration'(){
        setup:
        String c1 = """
            application_environments{ 
                dev {
                    override = true 
                    long_name = 'Dev'
                }
            }
            a = 3
            b = "hi" 
            c = true 
        """

        String c2 = """
            a = 4
            application_environments{ 
                dev {
                    long_name = 'Develop'
                }
            }
        """
        when:
        TemplateConfigObject configObject = aggregate(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == ([
            application_environments:[
                dev:[
                    long_name:'Develop'
                ]
            ],
            a: 3,
            b: "hi",
            c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 3 tier override Configuration'(){
        setup:
        String c1 = """
            application_environments{ 
                dev {
                    override = true 
                    long_name = 'Dev'
                }
            }
            a = 3
            b = "hi" 
            c = true 
        """

        String c2 = """
            a = 4
            application_environments{ 
                dev {
                    long_name = 'Develop'
                }
            }
        """

        String c3 = """
            a = 5
            application_environments{ 
                dev {
                    long_name = 'Development'
                }
            }
            """
        when:
        TemplateConfigObject configObject = aggregate(c1, c2, c3)

        then:// override and merge only apply to the next level
        configObject.config == ([
            application_environments:[
                dev:[
                    long_name:'Develop'
                ]
            ],
            a: 3,
            b: "hi",
            c: true
        ] as LinkedHashMap)
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier/level override to empty Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
                dev {
                    override = true
                    names {
                        Develop
                        Development
                        Dev
                        dev
                        devel
                        develop
                        development
                    }
                    long_name = 'Dev'
                }
            }
            a = 3
            b = "hi" 
            c = true 
        """

        String c2 = """
            a = 4
            application_environments{ 
                dev {
                    long_name = 'Develop'
                    names
                }
            }
            """
        when:
        TemplateConfigObject configObject = aggregate(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == [
            application_environments:[
                dev:[
                    long_name:'Develop', 
                    names:[:]
                ]
            ],
            a: 3,
            b: "hi",
            c: true
        ]
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier/level override to deeper Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
                dev {
                    override = true 
                    names 
                    long_name = 'Dev'
                }
            }
            a = 3
            b = "hi" 
            c = true 
        """

        String c2 = """
            a = 4
            application_environments{ 
                dev {
                    long_name = 'Develop'
                    names {
                        Develop {
                            merge = true 
                            name = 'Develop'
                        }
                        Development
                        Dev
                        dev
                        devel
                        develop
                        development
                    }
                }
            }
        """
        when:
        TemplateConfigObject configObject = aggregate(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == [
            application_environments:[
                dev:[
                    long_name:'Develop',
                    names:[
                        Develop: [
                            name:'Develop'
                        ],
                        Development:[:],
                        Dev:[:],
                        dev:[:],
                        devel:[:],
                        develop:[:],
                        development:[:]
                    ]
                ]
            ],
            a: 3,
            b: "hi",
            c: true
        ]
        !configObject.merge.isEmpty()
        (['application_environments.dev.names.Develop'] as Set).equals(configObject.merge)
        configObject.override.isEmpty()
    }

    def 'printJoin add'(){
        setup:
        PipelineConfig p = new PipelineConfig()

        when:
        p.join(new TemplateConfigObject(config:[a:1]))
        p.join(new TemplateConfigObject(config:[b:0]))
        
        then: 
        assert logs[0].contains("Configurations Added:\n- a set to 1")
        assert logs[1].contains("Configurations Added:\n- b set to 0")
        assert logs.findAll{ it.contains('Configurations Duplicated: None') }.size() == 2
        assert logs.findAll{ it.contains('Configurations Ignored: None') }.size() == 2
        assert logs.findAll{ it.contains('Subsequent May Merge: None') }.size() == 2
        assert logs.findAll{ it.contains('Subsequent May Override: None') }.size() == 2
    }

    def 'printJoin failed change'(){
        setup:
        PipelineConfig p = new PipelineConfig()
        TemplateConfigObject t = new TemplateConfigObject(config:[a:[b:1]])

        when:
        p.join(t)
        p.join(new TemplateConfigObject(config:[a:[b:2]]))

        then:
        assert logs[0].contains('Configurations Added:\n- a.b set to 1')
        assert logs[0].contains("Configurations Deleted: None")
        assert logs[0].contains('Configurations Changed: None')
        assert logs[0].contains('Configurations Duplicated: None')
        assert logs[0].contains('Configurations Ignored: None') 
        assert logs[0].contains('Subsequent May Merge: None') 
        assert logs[0].contains('Subsequent May Override: None') 

        assert logs[1].contains('Configurations Added: None') 
        assert logs[1].contains("Configurations Deleted: None")
        assert logs[1].contains('Configurations Changed: None')
        assert logs[1].contains('Configurations Duplicated: None')
        assert logs[1].contains('Configurations Ignored:\n- a.b')        
        assert logs[1].contains('Subsequent May Merge: None') 
        assert logs[1].contains('Subsequent May Override: None') 


    }

    def 'printJoin change'(){
        setup:
        PipelineConfig p = new PipelineConfig()
        TemplateConfigObject t = new TemplateConfigObject(config:[a:1])

        when:
        TemplateConfigObject configObject = aggregate("""
        a{
            override = true 
            b = 1
        }
        """, """
        a{
            b = 2
        }
        """)

        then:
        assert logs[0].contains("Configurations Added:\n- a.b set to 1")
        assert logs[0].contains("Configurations Deleted: None")
        assert logs[0].contains('Configurations Duplicated: None')
        assert logs[0].contains('Configurations Ignored: None') 
        assert logs[0].contains('Subsequent May Merge: None') 
        assert logs[0].contains('Subsequent May Override:\n- a') 

        assert logs[1].contains('Configurations Added: None') 
        assert logs[0].contains("Configurations Deleted: None")
        assert logs[1].contains("Configurations Changed:\n- a.b changed from 1 to 2")
    }

    def 'printJoin configuration Deleted'(){
        setup: 
        PipelineConfig p = new PipelineConfig()
        
        when: 
        p.join(TemplateConfigDsl.parse("""
            a{ 
                override = true
                y = true
                x = true 
            }
        """))
        p.join(TemplateConfigDsl.parse("""
            a{ 
                y = false 
            }
        """))

        then: 
        assert p.config.config.a == [ y: false ]
        assert logs[1].contains("Configurations Deleted:\n- a.x")
    }

    def 'ref: https://github.com/jenkinsci/templating-engine-plugin/issues/48'(){
        setup: 
        String c1 = """
            stages {
                stage_one {
                    override = true
                }
            }
        """
        String c2 = """
            stages {
                stage_two {
                    merge = true
                }
                stage_three {
                    override = true
                }
            }
        """
        TemplateConfigObject c = aggregate(c1, c2)
 
        when: 
        TemplateConfigDsl.serialize(c)

        then: 
        noExceptionThrown()        
    }

    TemplateConfigObject aggregate(String ... configs){
        PipelineConfig p = new PipelineConfig()
        configs.each{ p.join(TemplateConfigDsl.parse(it)) }
        return p.getConfig()
    }
}
