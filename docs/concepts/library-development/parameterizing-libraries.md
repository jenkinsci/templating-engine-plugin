# Parameterizing Libraries

One of the major benefits of organizing your pipeline code into libraries is the ability to reuse these libraries across different teams.

To achieve this level of reusability, it's best to externalize hard coded values as parameters that can be set from the Pipeline Configuration repository.

## Pass Parameters Through the Pipeline Configuration

When specifying a library to be loaded, users can pass arbitrary configurations to the library:

``` groovy title="pipeline_config.groovy"
libraries{
  example{ // (1)
    someField = "my value" // (2)
    nested{ // (3)
      someOtherField = 11 // (4)
    }
  }
}
```

1. The name of the library to be loaded
2. A root level library configuration option
3. A block name to pass nested configuration
4. A nested library configuration

## Parameter Structure and Type

Library parameters can take an arbitrary structure.
All parameters can be at the root level or a nested structure can be created to group related configurations together.
Library parameter values can be any serializable Groovy primitive.
Typically, parameters are boolean, numeric, String, or array.

## Accessing Library Configurations Within Steps

The Jenkins Templating Engine injects a `config` variable into each step. This `config` variable is a map whose keys are the library parameters that have been provided through the Pipeline Configuration.

The `config` variable is only resolvable within a Library Step and only contains the configuration for the step's library.

!!! note
    If you need to access the entire aggregated Pipeline Configuration, JTE injects a `pipelineConfig` variable that can be accessed anywhere.

## Validating Library Configurations

The Pipeline Configuration doesn't inherently perform type checking or validation.

Library developers can choose to provide a [Library Configuration File](./library-configuration-file.md) at the root of the library's directory which will assist with library parameter validation.
