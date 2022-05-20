# Autowired Variables

The JTE framework often makes use of autowired variables to share both configuration data and contextual information.

This page outlines the various autowired variables, their scope, and what data they provide.

## Overview

| Variable <img width=75/> | Description                                                                                                                 | Scope <img width=200/>                                                   |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `pipelineConfig`         | Represents the aggregated Pipeline Configuration                                                                            | Accessible everywhere                                                    |
| `jte`                    | The [Primitive Namespace](../concepts/pipeline-primitives/primitive-namespace.md) object                                    | Accessible everywhere                                                    |
| `config`                 | Represents a library's configuration provided by the aggregated Pipeline Configuration                                      | Within [Library Steps](../concepts/library-development/library-steps.md) |
| `stepContext`            | Enables step introspection. Especially helpful when using [Step Aliasing](../concepts/library-development/step-aliasing.md) | Within [Library Steps](../concepts/library-development/library-steps.md) |
| `hookContext`            | Represents contextual information for [Lifecycle Hooks](../concepts/library-development/lifecycle-hooks.md)                 | Within [Library Steps](../concepts/library-development/library-steps.md) |

## Autowired Global Variables

### `pipelineConfig`

The `pipelineConfig` is accessible from everywhere and allows access to the aggregated Pipeline Configuration as a [Map](https://docs.groovy-lang.org/latest/html/groovy-jdk/java/util/Map.html).

!!! example "Example Usage of `pipelineConfig`"
    An example of accessing the Pipeline Configuration via `pipelineConfig`:
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        keywords{
          foo = "bar"
        }
        random_field = 11
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        println pipelineConfig.keywords.foo
        println pipelineConfig.random_field
        ```

### `jte`

The `jte` variable represents the [Primitive Namespace](../concepts/pipeline-primitives/primitive-namespace.md).

All loaded Pipeline Primitives for a Run can be accessed via the `jte` variable

This is different from the `pipelineConfig` variable. The `pipelineConfig` variable gives a Map representation of the aggregated Pipeline Configuration whereas the `jte` variable allows access to the *actual Pipeline Primitive objects*.

!!! example "Example Usage of `jte`"
    Assume there's a `gradle` and an `npm` library that both contribute a `build()` step.

    By default, loading would result in the pipeline failing. However, you can perform [Step Overloading](../concepts/advanced/overloading-steps.md) by setting `jte.permissive_initialization` to `True`. 

    The `jte` would be used in this scenario to invoke the `build()` step from the `gradle` and `npm` libraries. 
    
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        jte{
          permissive_initialization = true
        }
        libraries{
          gradle
          npm
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        // invoke the gradle build step
        jte.libraries.gradle.build()
        // invoke the npm build step
        jte.libraries.npm.build()
        ```

!!! note "`jte` block vs `jte` variable"
    You may have noticed in the example above that a `jte{}` block is used in the Pipeline Configuration and a `jte` variable is used in the Pipeline Template.

    These are different things. 

    The `jte{}` block refers to framework-level feature flags as explained on the [Pipeline Configuration schema](./pipeline-configuration-schema.md) page.

    The `jte` variable refers to the [Pipeline Primitive Namespace](../concepts/pipeline-primitives/primitive-namespace.md) variable. 

### `steps`

The `steps` variable doesn't technically come from JTE.
It's a feature of all Jenkins Pipelines.

The `steps` variable allows direct access to invoke Jenkins Pipeline DSL Steps.

This variable is most commonly used when invoking Jenkins Pipeline DSL Steps from a [Library Class](../concepts/library-development/library-classes.md) or when [Overloading Steps](../concepts/advanced/overloading-steps.md).

## Autowired Library Step Variables

The following variables are only accessible within [Library Steps](../concepts/library-development/library-steps.md).

### `config`

The `config` variable represents the library configuration for the library that contributed the step as a [Map](https://docs.groovy-lang.org/latest/html/groovy-jdk/java/util/Map.html).

!!! example "Example Usage of `config`"
    Assume there's a `gradle` library that contributes a `build()` step.

    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        libraries{
          gradle{
            version = "6.3"
          }
        }
        ```
    === "Library build() Step"
        ``` groovy title="build.groovy"
        void call(){
          String gradleVersion = config.version
        }
        ```

### `hookContext`

The `hookContext` variable provides information about the current step to [Lifecycle Hooks](../concepts/library-development/lifecycle-hooks.md).

--8<-- "snippets/hookContext.md"

!!! example "Example `hookContext` usage"
    The following example shows how to use the `hookContext` variable so that a Lifecycle Hook only triggers after the `build()` step from the `gradle` library.

    === "Lifecycle Hook Step"
        ``` groovy
        @AfterStep({ hookContext.library == "gradle" && hookContext.step == "build" })
        void call(){
          println "running after the ${hookContext.library}'s ${hookContext.step} step"
        }
        ```

### `stageContext`

The `stageContext` variable provides information about the current [Stage](../concepts/pipeline-primitives/stages.md).

| Property | Type     | Description                                                                                                                                                                      |
| -------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`   | `String` | The name of the current Stage being executed. `null` if step isn't being executed as part of a Stage.                                                                            |
| `args`   | `Map`    | The [named parameters](http://docs.groovy-lang.org/docs/groovy-2.5.0-beta-1/html/documentation/#_named_arguments) provided to the Stage. An empty Map if no parameters provided. |

!!! example "Example usage of `stageContext`"
    The following example shows how to modify step behavior based upon Stage context.
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        libraries{
          npm // contributes unit_test()
          sonarqube // contributes static_code_analysis()
        }
        stages{
          ci{
            unit_test
            static_code_analysis
          }
        }
        ```
    === "NPM unit_test() Step"
        ``` groovy title="unit_test.groovy"
        void call(){
          if(stageContext.name == "ci"){
            println "running as part of the ci Stage"
          }
        }
        ```

### `stepContext`

The `stepContext` allows step introspection, such as querying the name of the library providing the step or the current name of the step.

| Property  | Type      | Description                                                                                                                                                         |
| --------- | --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `library` | `String`  | The name of the library that contributed the step                                                                                                                   |
| `name`    | `String`  | The **current name** of the step. May differ from the basename of the step's groovy file if using [Step Aliasing](../concepts/library-development/step-aliasing.md) |
| `isAlias` | `Boolean` | Is true when `stepContext.name` refers to an alias                                                                                                                  |

!!! example "Example usage of `stepContext`"
    === "Aliased Step"
        ``` groovy title="generic.groovy"
        @StepAlias(["build", "unit_test"])
        void call(){
          println "currently running as ${stepContext.name}"
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        build() // prints "currently running as build"
        unit_test() // prints "currently running as unit_test"
        ```
