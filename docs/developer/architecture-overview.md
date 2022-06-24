# Pipeline Initialization

``` mermaid
flowchart TD
    subgraph External to JTE
    WorkflowRun
    end
    subgraph JTE
    WorkflowRun-->|create|TemplateFlowDefinition
    TemplateFlowDefinition-->PipelineConfigurationAggregator
    TemplateFlowDefinition-->PipelineTemplateResolver
    TemplateFlowDefinition-->TemplatePrimitiveInjector
    end
```

Often, knowing which file to start with can be the biggest hurdle to understanding a new codebase.

In JTE, the class that kicks everything off is `TemplateFlowDefinition`.

When a pipeline kicks off, a [`WorkflowRun`][WorkflowRun] is created and eventually the `run()` method is called.
The `run()` method fetches the job's `FlowDefinition` and invokes `create()` to return a `FlowExecution`.

JTE's `TemplateFlowDefinition.create()` is where Pipeline Initialization is performed.

## Pipeline Initialization Phases

The steps JTE performs before the pipeline starts is referred to as **Pipeline Initialization**.

| Pipeline Initialization Stage       | Description                                                                                                                       |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| [Aggregate Pipeline Configurations] | Fetches the run's Pipeline Configuration files, parses them, and then merges them to create the Aggregated Pipeline Configuration |
| [Determine the Pipeline Template]   | Reads the Aggregated Pipeline Configuration and fetches the appropriate Pipeline Template                                         |
| [Create the Pipeline Primitives]    | Parses the Aggregated Pipeline Configuration to create the Pipeline Primitives                                                    |

<!--Link References-->
[WorkflowRun]: https://github.com/jenkinsci/workflow-job-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/job/WorkflowRun.java
[Aggregate Pipeline Configurations]: ./pipeline-initialization/aggregate-configs.md
[Determine the Pipeline Template]: ./pipeline-initialization/determine-template.md
[Create the Pipeline Primitives]: ./pipeline-initialization/inject-primitives.md
