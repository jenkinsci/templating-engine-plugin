# Stages

Stages help keep [Pipeline Templates](../pipeline-templates/index.md) DRY by grouping steps together for execution.

## Defining Stages

Stages are defined through the `stages{}` block. Each subkey references a [step](steps.md) to be executed.

## Stage Context

The `stageContext` variable allows a step to determine if it's being executed as part of a stage.

| Property            | Description                                                                                                                                  |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `stageContext.name` | The name of the stage being executed. Is set to `null` when the step execution is outside of a stage.                                        |
| `stageContext.args` | A map of named parameters passed to the stage. Is equal to an empty map when not within a stage execution or if no parameters were provided. |

### stageContext Example

Assume a library called `demo` is available within a configured [Library Source](../library-development/library-source.md).

=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    stages{
      continuous_integration{
        unit_test
      }
    }
    libraries{
      demo
    }
    ```
=== "Pipeline Template"
    ``` groovy title="Jenkinsfile"
    continuous_integration param1: "foo", param2: "bar"
    unit_test()
    ```
=== "Library Step"
    ``` groovy title="demo/steps/unit_test.groovy"
    void call(){
      println "stage name = ${stepContext.name}"
      println "param1 = ${stageContext.args.param1}"
      println "param2 = ${stageContext.args.param2}"
    }
    ```

The console log from this pipeline would look similar to:

``` text
...
stage name = continuous_integration
param1 = foo
param2 = bar
...
stage name = null 
param1 = null
param2 = null
```

## Use Cases

### Continuous Integration

A common example would be to create a continuous integration stage to keep templates DRY.

=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    ...
    stages{
      continuous_integration{
        unit_test
        static_code_analysis
        build
        scan_artifact
      }
    }
    ```
=== "Pipeline Template"
    ``` groovy title="Jenkinsfile"
    on_pull_request to: develop, {
      continuous_integration()
    }
    on_merge to: develop, {
      continuous_integration()
      deploy_to dev
    }
    on_merge to: main, {
      deploy_to prod
    }
    ```
