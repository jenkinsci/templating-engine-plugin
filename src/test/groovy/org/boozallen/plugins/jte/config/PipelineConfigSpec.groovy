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

class PipelineConfigSpec extends Specification {
    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared public String basePipelineConfig = null
    @Shared public def basePipelineConfigMap = null

    public PrintStream logger = Mock()
    public logs = [] 

    

    def setupSpec(){
        basePipelineConfig = PipelineConfig.baseConfigContentsFromLoader(groovyJenkinsRule.jenkins.getPluginManager()
                .uberClassLoader)

        basePipelineConfigMap = TemplateConfigDsl.parse(basePipelineConfig).config
    }

    def setup(){
        GroovyMock(TemplateLogger, global:true)

        _ * TemplateLogger.print(_, _) >> { s, c -> logs.push(s) }
    }

    def 'Join empty TemplateConfigObjects to PipelineConfig'(){
        setup:

        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(""))

        def configs = [""" """, """ """, """ """]

        configs.each{ c ->
            TemplateConfigObject config = TemplateConfigDsl.parse(c)
            if (config){
                p.join(config)
            }
        }

        when:
        TemplateConfigObject configObject = p.config

        then:
        configObject.config == [:]
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Flat Keys Configuration'(){
        setup:

        String c1 = """
            a = 3
            b = "hi" 
            c = true 
        """

        when:
        TemplateConfigObject configObject = combine(c1)

        then:
        configObject.config == (basePipelineConfigMap + [
            a: 3,
            b: "hi",
            c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys 2 tiers Configuration'(){
        setup:

        String c1 = """
            a = 3
            b = "hi" 
            c = true 
        """

        String c2 = """
            a = 4
        """
        when:
        TemplateConfigObject configObject = combine(c1, c2)

        then:
        configObject.config.equals(basePipelineConfigMap + [
            a: 3,
            b: "hi",
            c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier override Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
                @override dev {
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
        TemplateConfigObject configObject = combine(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == (basePipelineConfigMap + [
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
                @override dev {
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
        TemplateConfigObject configObject = combine(c1, c2, c3)

        then:// override and merge only apply to the next level
        configObject.config == (basePipelineConfigMap + ([
            application_environments:[
                dev:[
                    long_name:'Develop'
                ]
            ],
            a: 3,
            b: "hi",
            c: true
        ] as LinkedHashMap))
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier/level override to empty Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
                @override dev {
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
        TemplateConfigObject configObject = combine(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == (basePipelineConfigMap + [
            application_environments:[
                dev:[
                    long_name:'Develop', 
                    names:[:]
                ]
            ],
            a: 3,
            b: "hi",
            c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier/level override to deeper Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
                @override dev {
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
                        @merge Develop {
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
        TemplateConfigObject configObject = combine(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == (basePipelineConfigMap + [
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
        ])
        !configObject.merge.isEmpty()
        (['application_environments.dev.names.Develop'] as Set).equals(configObject.merge)
        configObject.override.isEmpty()
    }

    def 'printJoin false-positive change fix'(){
        setup:
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))

        when:
        p.join(new TemplateConfigObject(config:[a:1]))
        p.join(new TemplateConfigObject(config:[b:0, a:1]))

        then:
        assert logs[0].contains("Configurations Added:\n- a set to 1")
        assert logs[0].contains("Configurations Deleted: None")
        assert logs[0].contains("Configurations Changed: None")
        assert logs[0].contains("Configurations Duplicated: None")
        assert logs[0].contains("Configurations Ignored: None")
        assert logs[0].contains("Subsequent May Merge: None")
        assert logs[0].contains("Subsequent May Override: None")

        assert logs[1].contains("Configurations Added:\n- b set to 0")
        assert logs[1].contains("Configurations Deleted: None")
        assert logs[1].contains("Configurations Changed: None")
        assert logs[1].contains("Configurations Duplicated:\n- a")
        assert logs[1].contains("Configurations Ignored: None")
        assert logs[1].contains("Subsequent May Merge: None")
        assert logs[1].contains("Subsequent May Override: None")
    }

    def 'printJoin add'(){
        setup:
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))

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
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))
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
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))
        TemplateConfigObject t = new TemplateConfigObject(config:[a:1])

        when:
        TemplateConfigObject configObject = combine("""
        @override a{
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

    def 'override root key'(){
        setup: 
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))
        
        when: 
        p.join(TemplateConfigDsl.parse("@override a = 1"))
        p.join(TemplateConfigDsl.parse("a = 2"))

        then: 
        assert p.config.config.a == 2 
    }

    def 'override nested key'(){
        setup: 
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))
        
        when: 
        p.join(TemplateConfigDsl.parse("""
            a{ 
                @override y = true
                x = true 
            }
        """))
        p.join(TemplateConfigDsl.parse("""
            a{ 
                y = false 
            }
        """))

        then: 
        assert p.config.config.a.y == false 
        assert p.config.config.a.x == true  
    }

    def 'printJoin configuration Deleted'(){
        setup: 
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))
        
        when: 
        p.join(TemplateConfigDsl.parse("""
            @override a{ 
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


    /*
    helpers
     */


    // helper
    TemplateConfigObject combine(String ... configs){

        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))

        println(""); println( "=== combine ===" )
        configs[0..-1].eachWithIndex{ c, i ->
            TemplateConfigObject config = c ? TemplateConfigDsl.parse(c) : null
            if (config){
                println( "config:${i}" )
                print( TemplateConfigDsl.serialize(config) )
                println( "end config:${i}" )

                p.join(config)

                println(""); println( "end p.config:${i}" )
            }
        }

        // p.printChanges(System.out)

        println( "=== end combine ===" ); println("")

        p.config
    }
}
