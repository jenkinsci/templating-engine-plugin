# Aggregating Pipeline Configuration

## Build the Aggregated Pipeline Configuration

`PipelineConfigurationAggregator` handles fetching the configuration hierarchy from Governance Tiers and the job itself.

For example, a given job would get its parent chain all the way up to the Jenkins instance's Global Configuration.

``` mermaid
flowchart TD
  g[Global Configuration]
  g --> f_a[Folder A]
  g --> f_b[Folder B]
  f_a --> j_a[Job A]
  f_a --> j_b[Job B]
  f_b --> j_c[JobC]
  f_b --> j_d[Job D]
  style j_b fill:#FFD580,stroke:#000000
```

Turns into:

``` mermaid
flowchart LR
  g[Global Configuration]
  g --> f_a[Folder A]
  f_a --> j_b[Job B]
  style j_b fill:#FFD580,stroke:#000000
```

If a Governance Tier or the job itself have a Pipeline Configuration, the DSL is parsed into a corresponding `PipelineConfigurationObject`.

These `PipelineConfigurationObject`s are then added together to create the Aggregated Pipeline Configuration.

``` mermaid
flowchart LR
  g[Global Configuration]
  gc[Global Pipeline Configuration]
  ac[Folder A Pipeline Config]
  jc[Job Pipeline Config]
  agg[Aggregated Pipeline Config]
  g --> f_a[Folder A]
  f_a --> j_b[Job B]
  style j_b fill:#FFD580,stroke:#000000
  gc <-.- g
  ac <-.- f_a
  jc <-.- j_b
  gc -.->|  +  |ac
  ac -.->|  +  |jc
  jc -.->|  =  |agg
```

!!! note

    `PipelineConfigurationObject` overrides the plus operator so adding them together [merges two configurations](/concepts/pipeline-configuration/merging-configs.md).

## Pipeline Configuration Parsing

The Pipeline Configuration Syntax is a custom Domain-Specific Language (DSL).

The `org.boozallen.plugins.jte.init.governance.dsl` package has all the classes related to Pipeline Configuration parsing.

A Pipeline Configuration file is executed as a Groovy Script with a custom binding that makes the `env` variable available to the configuration file.

The Script is compiled using `PipelineConfigurationBuilder` as the script base class which overrides `methodMissing`, `propertyMissing`, and `setProperty` to build the `PipelineConfigurationObject` defined by the DSL.

This script is executed in the same groovy sandbox as Jenkins Pipelines to secure the script execution.

!!! note Groovy Runtime Metaprogramming

    To learn more about `methodMissing`, `propertyMissing`, and `setProperty` check out [Groovy Runtime Metaprogramming](https://groovy-lang.org/metaprogramming.html#_runtime_metaprogramming).
