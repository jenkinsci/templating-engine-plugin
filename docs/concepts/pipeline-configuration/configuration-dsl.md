# Pipeline Configuration Syntax

This page will cover the mechanics of JTE's Pipeline Configuration DSL.

## Motivation

Originally, the JTE Pipeline Configuration was written in more standard structures like JSON or YAML.

| Structure | Challenge                                                                                                     |
|-----------|---------------------------------------------------------------------------------------------------------------|
| JSON      | Too verbose to be comfortable writing.                                                                        |
| YAML      | Users would frequently make errors with YAML syntax that resulted in a different configuration than expected. |

In the end, a [Groovy](https://groovy-lang.org/documentation.html) DSL provided the best of both words in terms of verbosity and forgiveness.

Over time, the features made available through a custom DSL became useful.

## Data Structure Storage

While not required to understand the DSL, it can accelerate your learning if you're familiar with LinkedHashMaps
The Pipeline Configuration syntax is a nested builder language that relies on Blocks and Properties to build a LinkedHashMap representing the configuration.

## Property Setting

Properties of the Pipeline Configuration are set using Groovy's [Variable Assignment](https://groovy-lang.org/semantics.html) syntax.

=== "Pipeline Configuration DSL"
    ``` groovy title="pipeline_config.groovy"
    foo = "bar"
    ```
=== "Resulting `pipelineConfig`"
    ``` groovy
    assert pipelineConfig == [ foo: "bar" ]
    ```

!!! danger "Don't Declare Variables"
    The DSL relies on `setProperty(String propertyName, Object value)` being executed to persist the Pipeline Configuration property values.

    Take the following example: 

    === "Pipeline Configuration DSL"
        ``` groovy title="pipeline_config.groovy"
        x = "x" 
        String y = "y"
        ```
    === "Resulting `pipelineConfig`"
        ``` groovy
        assert pipelineConfig == [ x: "x" ]
        ```

    The `y` value is not persisted. 

## Blocks

The Pipeline Configuration DSL supports nested properties using Blocks.

=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    a{
      x = 1,
      y = 2
    }
    ```
=== "Resulting `pipelineConfig`"
    ``` groovy
    assert pipelineConfig == [
      a: [
        x: 1
        y: 2
      ]
    ]
    ```

### Empty Blocks and Unset Properties

A special case is empty blocks and unset properties. Both situations result in an empty map being set in the Pipeline Configuration.

=== "Pipeline Config"
    ``` groovy title="pipeline_config.groovy"
    a{
      x = 1
      y{}
      z
    }
    ```
=== "Resulting `pipelineConfig`"
    ``` groovy
    assert pipelineConfig == [
      a: [
        x: 1,
        y: [:],
        z: [:]
      ]
    ]
    ```

## Pipeline Governance Annotations

To support [Pipeline Governance](../pipeline-governance/index.md), the Pipeline Configuration DSL uses special annotations to control which aspects of the configuration the **next configuration in the [Configuration Hierarchy](../pipeline-governance/configuration-hierarchy.md)** is able to modify.

These annotations are called `@override` and `@merge` and both can be placed on a block and property.

=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    @merge a{
      x = 1
      @override y = 2
    }
    ```

!!! tip "Learn More"
    To learn more about how these annotations work, check out [Merging Pipeline Configurations](./merging-configs.md)
