---
hide: 
  - toc
---

# Overview

Pipeline Primitives are objects that can be defined from the [Pipeline Configuration](../pipeline-configuration/index.md) and accessed from a [Pipeline Template](../pipeline-templates/index.md).

Pipeline Primitives exist to make Pipeline Templates easier to write, easier to read, and easier to share across teams.

## Pipeline Primitive Types

| Primitive Type                                            | Description                                                                               |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------|
| [Steps](./steps.md)                                       | Define a step of the pipeline, typically to be invoked from the Pipeline Template.         |
| [Stages](./stages.md)                                     | Group steps together to keep templates DRY.                                               |
| [Application Environments](./application-environments.md) | Encapsulate environmental context                                                         |
| [Keywords](./keywords.md)                                 | Declare variables from the Pipeline Configuration for use in Pipeline Templates and steps |
