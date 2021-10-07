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
package org.boozallen.plugins.jte.job

import static org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext.JOB

import hudson.FilePath
import hudson.model.Action
import hudson.model.Item
import hudson.model.Queue
import hudson.model.TaskListener
import hudson.tasks.ArtifactArchiver
import org.boozallen.plugins.jte.init.PipelineDecorator
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint
import org.jenkinsci.plugins.workflow.flow.DurabilityHintProvider
import org.jenkinsci.plugins.workflow.flow.GlobalDefaultFlowDurabilityLevel
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.cps.CpsFlowFactoryAction2

/**
 * A custom {@link FlowDefinition} that initializes the pipeline using the {@link PipelineDecorator} prior to returning
 * a {@link CpsFlowExecution} representing the pipeline template
 */
@PersistIn(JOB)
abstract class TemplateFlowDefinition extends FlowDefinition {

    private static final String GENERATED_PIPELINE_NAME = 'JTE-Jenkinsfile'

    PipelineDecorator decorator

    static FlowDurabilityHint determineFlowDurabilityHint(FlowExecutionOwner owner){
        Queue.Executable exec = owner.getExecutable()
        if (!(exec instanceof WorkflowRun)) {
            throw new IllegalStateException("inappropriate context")
        }
        FlowDurabilityHint hint = (exec instanceof Item) ? DurabilityHintProvider.suggestedFor((Item)exec) : GlobalDefaultFlowDurabilityLevel.getDefaultDurabilityHint()
        return hint
    }

    @Override
    FlowExecution create(FlowExecutionOwner owner, TaskListener listener, List<? extends Action> actions) throws Exception {
        for (Action a : actions) {
            if (a instanceof CpsFlowFactoryAction2) {
                return ((CpsFlowFactoryAction2) a).create(this, owner, actions)
            }
        }
        FlowDurabilityHint hint = determineFlowDurabilityHint(owner)
        String template = initializeJTE(owner)

        if (decorator.config.getJteBlockWrapper().archive_generated_pipeline) {
            archiveGeneratedPipeline(owner, template)
        }

        return new CpsFlowExecution(template, true, owner, hint)
    }

    private String initializeJTE(FlowExecutionOwner owner){
        PipelineDecorator decorator = new PipelineDecorator(owner)
        decorator.initialize() // runs the initialization process for JTE
        this.decorator = decorator
        String template = decorator.getTemplate()
        return template
    }

    private void archiveGeneratedPipeline(FlowExecutionOwner owner, String template) {
        TemplateLogger logger = new TemplateLogger(owner.getListener())
        try {
            FilePath templateFilePath = new FilePath(new File("${owner.getRootDir()}/${GENERATED_PIPELINE_NAME}"))
            templateFilePath.write(template, null)
            ArtifactArchiver archiver = new ArtifactArchiver(GENERATED_PIPELINE_NAME)
            archiver.perform(owner.getExecutable(), new FilePath(owner.getRootDir()), null, null, owner.getListener())
            logger.print('Archived Generated Pipeline')
        } catch (any) {
            List<String> output = ['Archival of generated pipeline failed']
            output << any.message
            output << any.stackTrace
            logger.printWarning(output.join('\n'))
            output = ['Logged Generated Pipeline']
            output << template
            logger.print(output.join('\n'))
        }
    }

}
