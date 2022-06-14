# Library Steps

Library Steps are a mechanism for modularizing pipeline functionality.

## Naming A Step

By default, the name of the step that's loaded is based on the filename without the `.groovy` extension.  

This can be modified using [Step Aliasing](./step-aliasing.md).

!!! tip "Best Practice"
    It's recommended that Step Aliasing only be used when actually necessary.

## The `call` Method

Most steps should implement the `call` method.

``` groovy title="library_step.groovy"
void call(){}
```

This makes it such that the step can be invoked via its name.

For example, a step named `build.groovy` that has implemented a `call` method can be invoked via `build()`.

<!-- markdownlint-disable -->
!!! question "Why the Call Method?"
    Curious readers commonly ask, "Why the call method?"
    
    The answer comes from the [Groovy Call Operator](https://groovy-lang.org/operators.html#_call_operator).

    Essentially, `build()` is equivalent to `build.call()` in groovy.
<!-- markdownlint-restore -->

## Autowired Variables

All Library Steps are autowired with several variables:

| Variable       | Description                                                                                           |
|----------------|-------------------------------------------------------------------------------------------------------|
| `config`       | The library's block of configuration for the library that contributed the step.                       |
| `stepContext`  | Information about the step that's currently running.                                                  |
| `stageContext` | Information about the current [Stage](../pipeline-primitives/stages.md), if applicable                |
| `hookContext`  | If this step was triggered by a [Lifecycle Hook](./lifecycle-hooks.md), information about the trigger |

!!! info "Reference"
    For more information, check out the [Autowired Variables](../../reference/autowired-variables.md) page

## Method Parameters

Library Steps can accept method parameters just like any other method.

!!! example "Library Step Method Parameters"
    === "Library Step"
        ``` groovy title="printMessage.groovy"
        void call(String message){
          println "here's your message: ${message}"
        }
        ```
    === "Step Invocation"
        ``` groovy
        printMessage("hello, world!")
        ```

<!-- markdownlint-disable -->
!!! danger "Be Careful!"
    Library Steps that accept method parameters run a high risk of breaking the interoperability of the [Pipeline Template](../pipeline-templates/index.md).

    Imagine the scenario where the Pipeline Template invokes a `build()` step and the same template is intended to be used across teams that may be using different tools, such as `gradle` and `npm`.
    
    If the `gradle` library's `build()` step accepts a set of parameters and the `npm` library's `build()` step doesn't then you won't be able to swap out the libraries interchangeably.

    Instead of method parameters, consider passing steps information via the [Pipeline Configuration](../pipeline-configuration/index.md) using the `config` variable.

    Check out the [Parameterizing Libraries](./parameterizing-libraries.md) page to learn more.
<!-- markdownlint-restore -->

The exception to the rule of thumb regarding method parameters is when the method parameters are Pipeline Primitives.
This works because the parameters can then be interchanged safely along with the implementation of the step that's accepting them as an argument.

The most common example is creating a deployment step.
Frequently, teams will create a `deploy_to` step that accepts an [Application Environment](../pipeline-primitives/application-environments.md) as an argument.

!!! example "Deployment Steps"
    The following Pipeline Template, Pipeline Configuration, and Deployment step demonstrate a safe use of a library step accepting a method parameter.
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        unit_test()
        build()
        deploy_to dev
        smoke_test()
        deploy_to prod
        ```
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        libraries{
          npm     // contributes unit_test, build
          cypress // contributes integration_test
          ansible // contributes deploy_to
        }
        application_environments{
          dev{
            ip = "1.1.1.1"
          }
          prod{
            ip = "2.2.2.2"
          }
        }
        ```
    === "Deployment Step"
        ``` groovy title="ansible/steps/deploy_to.groovy"
        void call(app_env){
          println "deploying to the ip: ${app_env.ip}"
        }
        ```

## Advanced Topics

This page has covered the basics, if you're ready for more check out the following pages:

| Topic                                                 | Description                                         |
|-------------------------------------------------------|-----------------------------------------------------|
| [Lifecycle Hooks](./lifecycle-hooks.md)               | Learn how to trigger Library Steps implicitly.      |
| [Multi-Method Library Steps](./multi-method-steps.md) | Learn how to define more than one method in a step. |
| [Step Aliasing](./step-aliasing.md)                   | Learn how to call the same step by multiple names.  |
