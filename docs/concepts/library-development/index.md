# Overview

Pipeline Libraries are the foundation of the Jenkins Templating Engine to allow [Pipeline Templates](../pipeline-templates/index.md) to be shared across teams.

Libraries provide [steps](../pipeline-primitives/steps.md) that can be invoked from templates.

!!! tip "Library Repository Scaffold"
    Check out this [starter repository](https://github.com/steven-terrana/jte-library-scaffold) to help you get off on the right foot.

## Loading Libraries

The `libraries{}` block within the [Pipeline Configuration](../pipeline-configuration/index.md) defines which libraries will be loaded for a particular Pipeline Run.

## Learn More

| Topic                                                         | Description                                                                   |
|---------------------------------------------------------------|-------------------------------------------------------------------------------|
| [Library Structure](./library-structure.md)                   | Learn how files are organized within a library.                               |
| [Library Steps](./library-steps.md)                           | Learn how to add steps to a library.                                          |
| [Library Resources](./library-resources.md)                   | Learn how to use static assets from within Library Steps                      |
| [Library Classes](./library-classes.md)                       | Learn how to define classes within a library.                                 |
| [Parameterizing Libraries](./parameterizing-libraries.md)     | Learn how to make libraries configurable from the Pipeline Configuration      |
| [Library Configuration File](./library-configuration-file.md) | Learn how to validate library parameters using the library configuration file |
| [Library Sources](./library-source.md)                        | Learn how to make a library discoverable for loading.                         |
| [Lifecycle Hooks](./lifecycle-hooks.md)                       | Learn how to register steps for implicit invocation.                          |
| [Multi-Method Library Steps](./multi-method-steps.md)         | Learn how to define multiple methods per step.                                |
| [Step Aliasing](./step-aliasing.md)                           | Learn how to invoke the same step by multiple names.                          |
