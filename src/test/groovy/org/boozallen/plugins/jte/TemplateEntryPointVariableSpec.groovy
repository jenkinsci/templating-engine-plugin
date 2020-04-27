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

package org.boozallen.plugins.jte

import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.binding.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.binding.injectors.*
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigDsl
import org.boozallen.plugins.jte.config.PipelineConfig
import org.boozallen.plugins.jte.config.ScmPipelineConfigurationProvider
import org.boozallen.plugins.jte.config.TemplateConfigException
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.utils.RunUtils
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import hudson.ExtensionList
import hudson.Extension
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.junit.Rule
import org.jvnet.hudson.test.GroovyJenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges


class TemplateEntryPointVariableSpec extends Specification {

    @Rule GroovyJenkinsRule jenkins = new GroovyJenkinsRule()

    /***********************************
     Pipeline Configuration Aggregation 
    ***********************************/
    @WithoutJenkins
    @ConfineMetaClassChanges([TemplateEntryPointVariable])
    def "configs on folders aggregated in reverse order"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        tepv.metaClass.getJobPipelineConfiguration = { null }
        
        TemplateConfigObject c1 = Mock()
        GovernanceTier t1 = Stub{ getConfig() >> c1 }
        TemplateConfigObject c2 = Mock()
        GovernanceTier t2 = Stub{ getConfig() >> c2 }

        GroovySpy(GovernanceTier, global: true)
        GovernanceTier.getHierarchy() >> [ t2, t1 ]

        PipelineConfig pipelineConfig = Mock() 
        GroovySpy(PipelineConfig, global: true)
        new PipelineConfig() >> pipelineConfig

        when: 
        tepv.aggregateTemplateConfigurations()

        then: 
        1 * pipelineConfig.join(c1) 

        then: 
        1 * pipelineConfig.join(c2) 
    }

    @WithoutJenkins
    @ConfineMetaClassChanges([TemplateEntryPointVariable])
    def "configs on folders aggregated prior to job configuration"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        TemplateConfigObject jobConfig = Mock()
        tepv.metaClass.getJobPipelineConfiguration = { jobConfig }
        
        TemplateConfigObject c1 = Mock()
        GovernanceTier t1 = Mock()
        t1.getConfig() >> c1 
        TemplateConfigObject c2 = Mock()
        GovernanceTier t2 = Mock()
        t2.getConfig() >> c2 

        GroovySpy(GovernanceTier, global: true)
        GovernanceTier.getHierarchy() >> [ t2, t1 ]

        PipelineConfig pipelineConfig = Mock()
        GroovySpy(PipelineConfig, global: true)
        new PipelineConfig() >> pipelineConfig

        when: 
        tepv.aggregateTemplateConfigurations()

        then: 
        1 * pipelineConfig.join(c1) 

        then: 
        1 * pipelineConfig.join(c2) 
        
        then: 
        1 * pipelineConfig.join(jobConfig)
    }

    @WithoutJenkins
    @ConfineMetaClassChanges([TemplateEntryPointVariable])
    def "job configuration not joined when null"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        tepv.metaClass.getJobPipelineConfiguration = { null }
        GroovySpy(GovernanceTier, global: true)
        GovernanceTier.getHierarchy() >> []
        PipelineConfig pipelineConfig = Mock()
        GroovySpy(PipelineConfig, global: true)
        new PipelineConfig() >> pipelineConfig

        when: 
        tepv.aggregateTemplateConfigurations()

        then: 
        0 * pipelineConfig.join(_)
    }

    @WithoutJenkins
    def "getJobPipelineConfiguration - TemplateFlowDefinition - is present"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        TemplateFlowDefinition flowDef = Stub{ getPipelineConfig() >> "_" }        
        WorkflowJob job = GroovyStub{ getDefinition() >> flowDef }
        GroovyMock(RunUtils, global: true) 
        RunUtils.getJob() >> job
        
        TemplateConfigObject jobConfig = Mock() 
        GroovyMock(TemplateConfigDsl, global: true)
        TemplateConfigDsl.parse(_) >> jobConfig         

        expect: 
        tepv.getJobPipelineConfiguration() == jobConfig
    }

    @WithoutJenkins
    def "getJobPipelineConfiguration - TemplateFlowDefinition - is not present"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        TemplateFlowDefinition flowDef = Stub{ getPipelineConfig() >> null }        
        WorkflowJob job = GroovyStub{ getDefinition() >> flowDef }
        GroovyMock(RunUtils, global: true) 
        RunUtils.getJob() >> job
        
        GroovyMock(TemplateConfigDsl, global: true)
        TemplateConfigDsl.parse(_) >> null         

        expect: 
        tepv.getJobPipelineConfiguration() == null
    }

    @WithoutJenkins
    def "getJobPipelineConfiguration - not TemplateFlowDefinition - is present in SCM"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        WorkflowJob job = GroovyMock()
        GroovyMock(RunUtils, global: true)
        RunUtils.getJob() >> job

        FileSystemWrapper fsw = Stub{
            getFileContents(ScmPipelineConfigurationProvider.CONFIG_FILE, _, _) >> "_"
        }
        GroovyMock(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> fsw
        
        TemplateConfigObject jobConfig = Mock()
        GroovyMock(TemplateConfigDsl, global: true)
        TemplateConfigDsl.parse(_) >> jobConfig

        expect: 
        tepv.getJobPipelineConfiguration() == jobConfig
    }

    @WithoutJenkins
    def "getJobPipelineConfiguration - not TemplateFlowDefinition - is not present in SCM"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        WorkflowJob job = GroovyMock()
        GroovyMock(RunUtils, global: true)
        RunUtils.getJob() >> job

        FileSystemWrapper fsw = Stub{
            getFileContents(ScmPipelineConfigurationProvider.CONFIG_FILE, _, _) >> "_"
        }
        GroovyMock(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> fsw
        
        GroovyMock(TemplateConfigDsl, global: true)
        TemplateConfigDsl.parse(_) >> null

        expect: 
        tepv.getJobPipelineConfiguration() == null
    }

    /***********************
     Binding Initialization 
    ***********************/
    @Extension static class Injector extends TemplatePrimitiveInjector{}    

    @ConfineMetaClassChanges([TemplatePrimitiveInjector])
    def "Aggregated pipeline configuration passed to doInject, then doPostInject, then binding locked"(){
        setup:
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        TemplateConfigObject configObj = Mock()
        PipelineConfig pipelineConfig = Stub{ getConfig() >> configObj }
        TemplateBinding binding = Mock() 
        CpsScript script = GroovyMock{ getBinding() >> binding }

        def injector = GroovyMock(Injector, global:true) 

        ExtensionList<TemplatePrimitiveInjector> injectors = new ExtensionList<TemplatePrimitiveInjector>(jenkins.jenkins, Injector)
        injectors.removeAll() 
        injectors.add(injector)

        TemplatePrimitiveInjector.metaClass.static.all = {
            return injectors
        }

        when: 
        tepv.initializeBinding(pipelineConfig, script)

        then: 
        1 * injector.doInject(configObj, _)

        then: 
        1 * injector.doPostInject(configObj, _)

        then: 
        1 * binding.lock() 
    }

    def "All injectors are found in real job"(){
        setup: 
        ExtensionList<TemplatePrimitiveInjector> injectors

        when: 
        injectors = TemplatePrimitiveInjector.all() 

        then: 
        injectors.get(ApplicationEnvironmentInjector) != null 
        injectors.get(KeywordInjector) != null 
        injectors.get(LibraryLoader) != null 
        injectors.get(StageInjector) != null         
    }

    /*******************
     Template Selection 
    *******************/
    @WithoutJenkins
    def "Pipeline Job: template used if provided"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()
        String template = "job template" 
        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(TemplateFlowDefinition){
                getTemplate() >> template 
            }
        }

        GroovySpy(TemplateLogger, global:true) 
        PrintStream logger = Mock() 
        TemplateLogger.print(*_) >> { args -> 
            logger.println(args[0])
        }

        String result 

        Map config = [:]

        when: 
        result = tepv.getTemplate(config) 

        then: 
        result == template 
        1 * logger.println("Obtained Pipeline Template from job configuration")
        
    }
    
    @WithoutJenkins
    def "Pipeline Job: inherit default pipeline template"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(TemplateFlowDefinition){
                getTemplate() >> null 
            }
        }

        GroovyMock(GovernanceTier, global: true) 
        String template = "job template" 
        GovernanceTier.getHierarchy() >> [ 
            Mock(GovernanceTier){
                getJenkinsfile() >> template 
            }
        ]

        Map config = [:]

        String result 
        
        when: 
        result = tepv.getTemplate(config) 

        then: 
        result == template 
    }

    @WithoutJenkins
    def "use scm Jenkinsfile if allowed"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        String template = "job template" 
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents("Jenkinsfile", _, _) >> template 
        }

        Map config = [ allow_scm_jenkinsfile: true ]

        String result 
        
        when: 
        result = tepv.getTemplate(config) 

        then: 
        result == template
    }

    @WithoutJenkins
    def "warn about Jenkinsfile if present and not allowed"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        String jobTemplate = "job template" 
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents("Jenkinsfile", _, _) >> jobTemplate 
        }

        GroovyMock(GovernanceTier, global: true) 
        String tierTemplate = "job template" 
        GovernanceTier.getHierarchy() >> [ 
            Mock(GovernanceTier){
                getJenkinsfile() >> tierTemplate 
            }
        ]

        PrintStream logger = Mock()
        GroovyMock(TemplateLogger, global: true)
        TemplateLogger.printWarning(*_) >> { args -> 
            logger.println(args[0]) 
        }

        Map config = [
            allow_scm_jenkinsfile: false
        ]

        String result 
        
        when: 
        result = tepv.getTemplate(config) 

        then: 
        result == tierTemplate
        1 * logger.println("Repository provided Jenkinsfile that will not be used, per organizational policy.")

    }

    @WithoutJenkins
    def "able to select named template"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents(*_) >> null 
        }

        String templateName = "example"
        String template = "tier 1 template"
        GroovySpy(GovernanceTier, global:true)
        GovernanceTier.getHierarchy() >> [ 
            Mock(GovernanceTier){
                getTemplate(templateName) >> template 
            }
        ] 

        Map config = [ 
            pipeline_template: templateName
        ]

        String result 

        when: 
        result = tepv.getTemplate(config)

        then: 
        result == template
    }
    
    @WithoutJenkins
    def "missing named template throws exception"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents(*_) >> null 
        }

        String templateName = "example"
        GroovySpy(GovernanceTier, global:true)
        GovernanceTier.getHierarchy() >> [ 
            Mock(GovernanceTier){
                getTemplate(templateName) >> null 
            }
        ] 

        Map config = [ 
            pipeline_template: templateName
        ]

        String result 

        when: 
        tepv.getTemplate(config)

        then: 
        def ex = thrown(TemplateConfigException) 
        ex.getMessage() == "Pipeline Template ${config.pipeline_template} could not be found in hierarchy."
    }

    @WithoutJenkins
    def "named template selected from more specific governance tier"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents(*_) >> null 
        }

        String templateName = "example"

        String tier1Template = "tier 1 template"
        GovernanceTier tier1 = Mock{ 
            getTemplate(templateName) >> tier1Template
        }

        GovernanceTier tier2 = Mock{
            getTemplate(templateName) >> "tier 2 template" 
        }

        GroovySpy(GovernanceTier, global:true)
        GovernanceTier.getHierarchy() >> [ tier1, tier2 ] 

        Map config = [
            pipeline_template: templateName 
        ]

        String result 

        when: 
        result = tepv.getTemplate(config)

        then: 
        result == tier1Template
    }
    
    @WithoutJenkins
    def "default pipeline template selected from more specific governance tier"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents(*_) >> null 
        }

        String tier1Template = "tier 1 jenkinsfile"
        GovernanceTier tier1 = Mock{ 
            getJenkinsfile() >> tier1Template
        }

        GovernanceTier tier2 = Mock{
            getJenkinsfile() >> "tier 2 jenkinsfile" 
        }

        GroovySpy(GovernanceTier, global:true)
        GovernanceTier.getHierarchy() >> [ tier1, tier2 ] 

        Map config = [:]

        String result 

        when: 
        result = tepv.getTemplate(config)

        then: 
        result == tier1Template
    }

    @WithoutJenkins
    def "no template throws exception"(){
        setup: 
        TemplateEntryPointVariable tepv = new TemplateEntryPointVariable()

        GroovySpy(RunUtils, global:true)
        RunUtils.getJob() >> GroovyMock(WorkflowJob){
            getDefinition() >> Mock(CpsFlowDefinition)
        }

        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromJob() >> Mock(FileSystemWrapper){
            getFileContents(*_) >> null 
        }

        GovernanceTier tier1 = Mock{ 
            getTemplate(_) >> null 
            getJenkinsfile() >> null
        }

        GovernanceTier tier2 = Mock{
            getTemplate(_) >> null 
            getJenkinsfile() >> null 
        }

        GroovySpy(GovernanceTier, global:true)
        GovernanceTier.getHierarchy() >> [ ] 

        Map config = [:]

        String result 

        when: 
        tepv.getTemplate(config)

        then: 
        def ex = thrown(TemplateConfigException)
        ex.getMessage() == "Could not determine pipeline template."
    }
}
