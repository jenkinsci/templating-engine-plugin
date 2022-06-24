---
hide:
  - toc
---

# Job Types

`TemplateFlowDefinition` is a custom implementation of [`FlowDefinition`](https://github.com/jenkinsci/workflow-api-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/flow/FlowDefinition.java) that allows JTE to perform [Pipeline Initialization](../architecture-overview.md#pipeline-initialization).

## Class Structure

``` mermaid
flowchart TD
  subgraph Organization Job
  TemplateMultiBranchProjectFactory
  end
  subgraph MultiBranch Job
  TemplateBranchProjectFactory-->|creates|MultiBranchTemplateFlowDefinition
  end
  subgraph Pipeline Job
  ConsoleAdHocTemplateFlowDefinitionConfiguration-.->|configures|AdHocTemplateFlowDefinition
  ScmAdHocTemplateFlowDefinitionConfiguration-.->|configures|AdHocTemplateFlowDefinition
  end
  TemplateFlowDefinition
  AdHocTemplateFlowDefinition-->|extends|TemplateFlowDefinition
  MultiBranchTemplateFlowDefinition-->|extends|TemplateFlowDefinition
  TemplateMultiBranchProjectFactory-->|creates|TemplateBranchProjectFactory
```

| Class                               | Purpose                                               |
|-------------------------------------|-------------------------------------------------------|
| `TemplateFlowDefinition`            | Makes a pipeline a JTE pipeline                       |
| `AdHocTemplateFlowDefinition`       | Adds JTE as an option to Pipeline Jobs                |
| `MultiBranchTemplateFlowDefinition` | Enables Multi-Branch Pipelines to use JTE             |
| `TemplateBranchProjectFactory`      | Adds JTE as an option to create Multi-Branch Projects |
| `TemplateMultiBranchProjectFactory` | Adds JTE as an option to create Organization Projects |
