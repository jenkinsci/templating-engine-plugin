# Determining the Pipeline Template

## Pipeline Template Resolution

`PipelineTemplateResolver` implements the [Pipeline Template Selection flow](/concepts/pipeline-governance/pipeline-template-selection.md).

## Pipeline Template Compilation

`PipelineTemplateCompiler` intercepts the compilation of the resolved Pipeline Template to wrap it in a try-catch-finally block that invokes the [Lifecycle Hooks](/concepts/library-development/lifecycle-hooks.md).
