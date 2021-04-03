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
package org.boozallen.plugins.jte.init.governance.config.dsl

import hudson.model.TaskListener
import org.boozallen.plugins.jte.util.TestUtil
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import spock.lang.Specification

class PipelineConfigurationObjectSpec extends Specification {

    ArrayList templateLogs = []
    PipelineConfigurationDsl dsl = new PipelineConfigurationDsl(GroovyMock(FlowExecutionOwner) {
        run() >> GroovyMock(WorkflowRun)
        getListener() >> GroovyMock(TaskListener) {
            getLogger() >> Mock(PrintStream) {
                println(_) >> { msg ->
                    templateLogs << msg[0]
                    println msg[0]
                }
            }
        }
        asBoolean() >> true
    })

    PipelineConfigurationObject aggregatedConfig

    def setup() {
        EnvActionImpl env = Mock()
        env.getProperty('someField') >> 'envProperty'

        GroovySpy(EnvActionImpl, global:true)
        EnvActionImpl.forRun(_) >> env

        aggregatedConfig = new PipelineConfigurationObject(dsl.getFlowOwner())
        aggregatedConfig.firstConfig = true

        /**
         * at beginning of test.. log to std out:
         * ============
         * name of test
         * ============
         */
        String name = specificationContext.currentIteration.name
        String header = name.collect { '=' }.join('')
        println "${header}\n${name}\n${header}"
    }

    def "First join results in correct aggregated config"() {
        given:
        aggregatedConfig += dsl.parse('''
            a = 1
            b = 2
            x{
                y = "whatever"
            }
        ''')

        expect:
        aggregatedConfig.config == [
            a: 1,
            b: 2,
            x: [
                y: 'whatever'
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a set to 1',
            '- b set to 2',
            '- x.y set to whatever',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "merge block: add key"() {
        given:
        String config1 = '@merge a{ x = 1 }'
        String config2 = 'a{ y = 2 }'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                x: 1,
                y: 2
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge:',
            '- a',
            'Subsequent May Override: None',

            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.y set to 2',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "merge block: can't override existing key"() {
        given:
        String config1 = '@merge a{ x = 1 }'
        String config2 = 'a{ x = 2 }'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                x: 1
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge:',
            '- a',
            'Subsequent May Override: None',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored:',
            '- a.x',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "merge block: log duplicated key"() {
        given:
        String config1 = '@merge a{ x = 1 }'
        String config2 = 'a{ x = 1 }'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                x: 1
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge:',
            '- a',
            'Subsequent May Override: None',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated:',
            '- a.x',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override block: change key value"() {
        given:
        String config1 = '@override a{ x = 1 }'
        String config2 = 'a{ x = 2 }'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                x: 2
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- a',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted: None',
            'Configurations Changed:',
            '- a.x changed from 1 to 2',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override block: leave block as-is if not declared"() {
        given:
        String config1 = '@override a{ x = 1 }'
        String config2 = 'b{ y = 1 }'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                x: 1
            ],
            b: [
                y: 1
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- a',

            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- b.y set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override block: delete previous configuration"() {
        given:
        String config1 = '''
        @override a{
            x = 1
            y = 2
        }
        '''
        String config2 = '''
        a{
            y = 3
        }
        '''

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                y: 3
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.x set to 1',
            '- a.y set to 2',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- a',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted:',
            '- a.x',
            'Configurations Changed:',
            '- a.y changed from 2 to 3',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override nested block"() {
        given:
        String config1 = '''
        a{
            @override b{
                c = 1
            }
        }
        '''
        String config2 = '''
        a{
            b{
                c = 2
            }
        }
        '''

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                b: [
                    c: 2
                ]
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.b.c set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- a.b',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted: None',
            'Configurations Changed:',
            '- a.b.c changed from 1 to 2',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override nested field"() {
        given:
        String config1 = '''
        a{
            b{
                @override c = 1
                d = 2
            }
        }
        '''
        String config2 = '''
        a{
            b{
                c = 2
                d = 4
            }
        }
        '''

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            a: [
                b: [
                    c: 2,
                    d: 2
                ]
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- a.b.c set to 1',
            '- a.b.d set to 2',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- a.b.c',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted: None',
            'Configurations Changed:',
            '- a.b.c changed from 1 to 2',
            'Configurations Duplicated: None',
            'Configurations Ignored:',
            '- a.b.d',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override root field"() {
        given:
        String config1 = '@override x = 1'
        String config2 = 'x = 2'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [ x: 2 ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- x',

            'Pipeline Configuration Modifications',
            'Configurations Added: None',
            'Configurations Deleted: None',
            'Configurations Changed:',
            '- x changed from 1 to 2',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "override root field with block"() {
        given:
        String config1 = '@override x = 1'
        String config2 = 'x{ y = 1 }'

        when:
        aggregatedConfig += dsl.parse(config1)
        aggregatedConfig += dsl.parse(config2)

        then:
        aggregatedConfig.config == [
            x: [
                y: 1
            ]
        ]
        TestUtil.assertOrder(templateLogs.join('\n'), [
            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- x set to 1',
            'Configurations Deleted: None',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override:',
            '- x',

            'Pipeline Configuration Modifications',
            'Configurations Added:',
            '- x.y set to 1',
            'Configurations Deleted:',
            '- x',
            'Configurations Changed: None',
            'Configurations Duplicated: None',
            'Configurations Ignored: None',
            'Subsequent May Merge: None',
            'Subsequent May Override: None'
        ])
    }

    def "GitHub Issue #174"() {
        given:
        String config = '''
        keywords{
          master = ~/[Mm](aster|ain)/
        }
        '''
        PipelineConfigurationObject obj = dsl.parse(config)

        when:
        String serialized = dsl.serialize(obj)
        dsl.parse(serialized)

        then:
        noExceptionThrown()
    }

}
