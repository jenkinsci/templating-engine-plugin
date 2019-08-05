package org.boozallen.plugins.jte

import hudson.Extension
import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.binding.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.config.PipelineConfig
import org.boozallen.plugins.jte.config.TemplateConfigDsl
import org.boozallen.plugins.jte.config.TemplateConfigException
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.utils.RunUtils
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import spock.lang.*
import hudson.model.*
import org.junit.*;
import org.jvnet.hudson.test.*;

class TemplateEntryPointVariableSpec extends Specification {
  @Shared
  @ClassRule
  @SuppressWarnings('JUnitPublicField')
  public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

  @Shared
  public ClassLoader classLoader;

  TemplateEntryPointVariable templateEntryPointVariable = new TemplateEntryPointVariable()

  def setupSpec(){
    classLoader = groovyJenkinsRule.jenkins.getPluginManager().uberClassLoader
  }

  def "getName returns 'template'"(){
    setup:
    String name = null

    when:
    name = templateEntryPointVariable.getName()

    then:
    name == TemplateEntryPointVariable.NAME
    notThrown(Exception)
  }

  def "getValue returns the existing value from script.binding" (){
    setup:
    CpsScript script = Mock(CpsScript)
    TemplateBinding binding = Mock(TemplateBinding)
    Script template = Mock(Script)

    2 * script.getBinding() >> { return binding }
    1 * binding.getVariable(TemplateEntryPointVariable.NAME) >> { return template }
    1 * binding.hasVariable(TemplateEntryPointVariable.NAME) >> { return true }

    Object value;
    when:
    value = templateEntryPointVariable.getValue(script)

    then:
    value == template
    notThrown(Exception)

  }

  def "getValue returns template and sets in binding; all subroutines mocked" (){
    setup:
    CpsScript script = Mock(CpsScript)
    groovy.lang.Binding bindingStart = Mock(groovy.lang.Binding)
    TemplateBinding binding = Mock(TemplateBinding)

    1 * script.getBinding() >> { return bindingStart }
    0 * bindingStart.getVariable(TemplateEntryPointVariable.NAME)
    1 * bindingStart.hasVariable(TemplateEntryPointVariable.NAME) >> { return false }

    TemplateEntryPointVariable t = templateEntryPointVariable = Spy(TemplateEntryPointVariable)
    1 * t.newTemplateBinding() >> { return binding }
    1 * script.setBinding(binding)

    PipelineConfig pipelineConfig = Mock(PipelineConfig)
    1 * t.newPipelineConfig() >> { return pipelineConfig }
    1 * t.aggregateTemplateConfigurations(_) >> { return }

    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    2 * pipelineConfig.getConfig() >> { return templateConfigObject }

    LinkedHashMap configMap = [:] as LinkedHashMap
    1 * templateConfigObject.getConfig() >> { return configMap }

    1 * binding.setVariable("pipelineConfig", configMap)
    1 * binding.setVariable("templateConfigObject", templateConfigObject)

    1 * t.initializeBinding( pipelineConfig, script) >> { return }
    GroovySpy(RunUtils, global:true)
    RunUtils.getClassLoader() >> {return classLoader }

    GroovySpy(TemplateScriptEngine, global: true)
    Script parseResult = Mock(Script)

    1 * TemplateScriptEngine.parse(_, binding) >> {return parseResult}
    1 * binding.setVariable(t.getName(), parseResult)

    Object value;
    when:
    value = templateEntryPointVariable.getValue(script)

    then:
    value == parseResult
    notThrown(Exception)

  }

  def "aggregateTemplateConfigurations 1 tier, no local config"(){
    setup:
    PipelineConfig pipelineConfigMain =  Mock(PipelineConfig)

    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    //1 * templateConfigObject.asBoolean() >> { return true }

    1 * pipelineConfigMain.join(templateConfigObject) >> { return }

    GovernanceTier tier = GroovyMock(GovernanceTier, global: true)
    tier.getConfig() >> { return templateConfigObject }

    1 * GovernanceTier.getHierarchy() >> { return [ tier ] }
    1 * GovernanceTier.getCONFIG_FILE() >> { return "pipeline_config.groovy" } // mocks GovernanceTier.CONFIG_FILE

    FileSystemWrapper fsw = GroovyMock(FileSystemWrapper, global: true)
    1 * FileSystemWrapper.createFromJob() >> { return fsw }
    1 * fsw.getFileContents(_, _, _) >> { return null }


    when:
    templateEntryPointVariable.aggregateTemplateConfigurations(pipelineConfigMain)

    then:
    notThrown(Exception)
  }

  def "aggregateTemplateConfigurations #tier_count tier, with local config"(){
    setup:
    PipelineConfig pipelineConfigMain =  Mock(PipelineConfig)

    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    //1 * templateConfigObject.asBoolean() >> { return true }

    tier_count * pipelineConfigMain.join(templateConfigObject) >> { return }

    def tiers = []

    for( int t = 0; t < tier_count; t++ ) {
      GovernanceTier tier = GroovyMock(GovernanceTier)
      1 * tier.getConfig() >> { return templateConfigObject }
      tiers << tier
    }

    GroovyMock(GovernanceTier, global: true)
    1 * GovernanceTier.getHierarchy() >> { return tiers }
    1 * GovernanceTier.getCONFIG_FILE() >> { return "pipeline_config.groovy" }

    FileSystemWrapper fsw = GroovyMock(FileSystemWrapper, global: true)
    1 * FileSystemWrapper.createFromJob() >> { return fsw }

    TemplateConfigObject appTemplate = Mock(TemplateConfigObject)
    String appTemplateContent = "_"
    1 * fsw.getFileContents("pipeline_config.groovy", _, _) >> { return appTemplateContent }

    GroovyMock(TemplateConfigDsl, global: true)
    1 * TemplateConfigDsl.parse(appTemplateContent) >> { return appTemplate }
    1 * pipelineConfigMain.join(appTemplate) >> { return }


    when:
    templateEntryPointVariable.aggregateTemplateConfigurations(pipelineConfigMain)

    then:
    notThrown(Exception)

    where:
    tier_count << [0, 1, 5]

  }

  @Extension
  public static class TemplatePrimitiveInjectorLocal extends TemplatePrimitiveInjector {
    static final public String DO_INJECT = "TemplatePrimitiveInjectorLocal-doInject"
    static final public String DO_POST_INJECT = "TemplatePrimitiveInjectorLocal-doPostInject"
    // Optional. delegate injecting template primitives into the binding to the specific
    // implementations of TemplatePrimitive
    static void doInject(TemplateConfigObject config, CpsScript script){
      script.getBinding().setVariable(DO_INJECT, TemplatePrimitiveInjectorLocal.newInstance())
    }

    // Optional. do post processing of the config and binding.
    static void doPostInject(TemplateConfigObject config, CpsScript script){
      script.getBinding().setVariable(DO_POST_INJECT, TemplatePrimitiveInjectorLocal.newInstance())
    }
  }

  def "initializeBinding" (){
    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    PipelineConfig pipelineConfigMain = Mock(PipelineConfig)
    1 * pipelineConfigMain.getConfig() >> { return templateConfigObject }

    CpsScript script = Mock(CpsScript)
    TemplateBinding templateBinding = Mock(TemplateBinding)

    3 * script.getBinding() >> { return templateBinding} //
    1 * templateBinding.lock() >> { return }
    1 * templateBinding.setVariable(TemplatePrimitiveInjectorLocal.DO_INJECT, _) >> { return }
    1 * templateBinding.setVariable(TemplatePrimitiveInjectorLocal.DO_POST_INJECT, _) >> { return }

    GroovySpy(TemplatePrimitiveInjector.Impl.class, global:true)
    1 * TemplatePrimitiveInjector.Impl.all() >> { return groovyJenkinsRule.jenkins.getExtensionList(TemplateEntryPointVariableSpec.TemplatePrimitiveInjectorLocal.class) }

    when:
    templateEntryPointVariable.initializeBinding(pipelineConfigMain, script)

    then:
    notThrown(Exception)

  }

  def "getTemplate from TemplateFlowDefinition" (){

    setup:
    String templateVar;
    String templateString = "the template";
    Map config = Mock(HashMap)

    TemplateFlowDefinition templateFlowDefinition = Mock(TemplateFlowDefinition)
    1 * templateFlowDefinition.getTemplate() >> { return templateString }
    FlowDefinition flowDefinition = templateFlowDefinition

    WorkflowJob job = GroovyMock(WorkflowJob)
    1 * job.getDefinition() >> {return flowDefinition }

    GroovyMock(RunUtils, global: true)
    1 * RunUtils.getJob() >> { return job}

    GroovyMock(TemplateLogger, global: true)
    1 * TemplateLogger.print("Obtained Pipeline Template from job configuration") >> {return }

    GroovyMock(FileSystemWrapper, global: true)
    0 * FileSystemWrapper.createFromJob()

    FileSystemWrapper fs = GroovyMock(FileSystemWrapper)
    0 * fs.getFileContents(_, _, _)

    GroovyMock(GovernanceTier, global: true)
    0 * GovernanceTier.getHierarchy()

    0 * config.get("pipeline_template")
    0 * config.get("allow_scm_jenkinsfile")

    when:
    templateVar = TemplateEntryPointVariable.getTemplate(config)

    then:
    notThrown(Exception)
    templateVar == templateString

  }

  def "getTemplate from app JenkinsFile and allow_scm_jenkinsfile" (){

    setup:
    String templateVar;
    String fileString = "the fileString";
    Map config = Mock(HashMap)//new HashMap()
    //config.allow_scm_jenkinsfile = true
    1 * config.get("allow_scm_jenkinsfile") >> {return true }
    FlowDefinition flowDefinition = Mock(FlowDefinition)

    WorkflowJob job = GroovyMock(WorkflowJob)
    1 * job.getDefinition() >> {return flowDefinition }

    GroovyMock(RunUtils, global: true)
    1 * RunUtils.getJob() >> { return job}

    GroovyMock(TemplateLogger, global: true)
    0 * TemplateLogger.print(_, _)
    0 * TemplateLogger.printWarning(_)

    GroovyMock(FileSystemWrapper, global: true)
    FileSystemWrapper fs = GroovyMock(FileSystemWrapper)
    1 * fs.getFileContents("Jenkinsfile", "Repository Jenkinsfile", false) >> {return fileString}

    1 * FileSystemWrapper.createFromJob() >> { return fs }

    GroovyMock(GovernanceTier, global: true)
    0 * GovernanceTier.getHierarchy()

    // config.pipeline_template
    0 * config.get("pipeline_template")


    when:
    templateVar = TemplateEntryPointVariable.getTemplate(config)

    then:
    notThrown(Exception)
    templateVar == fileString


  }

  def "getTemplate throws exception when app JenkinsFile and !allow_scm_jenkinsfile and !pipeline_template and no tiers" (){

    setup:
    String templateVar;
    String fileString = "the fileString";
    Map config = Mock(HashMap)//new HashMap()
    //config.allow_scm_jenkinsfile = false
    1 * config.get("allow_scm_jenkinsfile") >> {return false }
    FlowDefinition flowDefinition = Mock(FlowDefinition)

    WorkflowJob job = GroovyMock(WorkflowJob)
    1 * job.getDefinition() >> {return flowDefinition }

    GroovyMock(RunUtils, global: true)
    1 * RunUtils.getJob() >> { return job}

    GroovyMock(TemplateLogger, global: true)
    0 * TemplateLogger.print(_, _)
    1 * TemplateLogger.printWarning("Repository provided Jenkinsfile that will not be used, per organizational policy.")

    GroovyMock(FileSystemWrapper, global: true)
    FileSystemWrapper fs = GroovyMock(FileSystemWrapper)
    1 * fs.getFileContents("Jenkinsfile", "Repository Jenkinsfile", false) >> {return fileString}

    1 * FileSystemWrapper.createFromJob() >> { return fs }

    GroovyMock(GovernanceTier, global: true)
    1 * GovernanceTier.getHierarchy() >> { return [] as ArrayList }

    // config.pipeline_template
    1 * config.get("pipeline_template") >> { return null }


    when:
    templateVar = TemplateEntryPointVariable.getTemplate(config)

    then:
    def e = thrown(TemplateConfigException)
    e.getMessage() == "Could not determine pipeline template."

  }

  def "getTemplate throws exception when no app JenkinsFile and !pipeline_template and no tiers" (){

    setup:
    String templateVar;
    String fileString = null;
    Map config = Mock(HashMap)//new HashMap()
    //config.allow_scm_jenkinsfile = false
    0 * config.get("allow_scm_jenkinsfile") >> {return false }
    FlowDefinition flowDefinition = Mock(FlowDefinition)

    WorkflowJob job = GroovyMock(WorkflowJob)
    1 * job.getDefinition() >> {return flowDefinition }

    GroovyMock(RunUtils, global: true)
    1 * RunUtils.getJob() >> { return job}

    GroovyMock(TemplateLogger, global: true)
    0 * TemplateLogger.print(_, _)
    0 * TemplateLogger.printWarning("Repository provided Jenkinsfile that will not be used, per organizational policy.")

    GroovyMock(FileSystemWrapper, global: true)
    FileSystemWrapper fs = GroovyMock(FileSystemWrapper)
    1 * fs.getFileContents("Jenkinsfile", "Repository Jenkinsfile", false) >> {return fileString}

    1 * FileSystemWrapper.createFromJob() >> { return fs }

    GroovyMock(GovernanceTier, global: true)
    1 * GovernanceTier.getHierarchy() >> { return [] as ArrayList }

    // config.pipeline_template
    1 * config.get("pipeline_template") >> { return null }

    when:
    templateVar = TemplateEntryPointVariable.getTemplate(config)

    then:
    def e = thrown(TemplateConfigException)
    e.getMessage() == "Could not determine pipeline template."

  }
}
