# Overview

[Pipeline Templates](../pipeline-templates/index.md) are generic, tool-agnostic workflows that utilize [Pipeline Primitives](../pipeline-primitives/index.md) to become concrete for specific teams.

The Pipeline Configuration is what determines for a given Pipeline Run which Pipeline Template and which Pipeline Primitives should be used.

## Structure

JTE's Pipeline Configuration uses a [custom Domain-Specific Language (DSL)](./configuration-dsl.md) which after being parsed by JTE builds a [Map](https://docs.groovy-lang.org/latest/html/groovy-jdk/java/util/Map.html).

This DSL is a dynamic builder language.

It doesn't validate that block names and fields align to the [Pipeline Configuration Schema](../../reference/pipeline-configuration-schema.md) in any way.

If a block or field is declared that's not in the schema, it will simply be ignored during [Pipeline Initialization](../advanced/pipeline-initialization.md) such that no Pipeline Primitives are created.
The incorrect fields will still be accessible on the [`pipelineConfig` autowired variable](../../reference/autowired-variables.md##pipelineconfig)

## Script Security

The Pipeline Configuration file is parsed by executing it within the same [Groovy Sandbox](https://github.com/jenkinsci/script-security-plugin) that Jenkins pipelines use as well.

## Pipeline Configuration Location

### Configuration Hierarchy

Pipeline Configurations can be stored in the [Configuration Hierarchy](../pipeline-governance/configuration-hierarchy.md) on [Governance Tiers](../pipeline-governance/governance-tier.md).

!!! note "Merging Pipeline Configurations"
    When more than one Pipeline Configuration is present for a given Pipeline Run, they're merged according to the rules outlined on [Merging Configurations](./merging-configs.md).

### Job-Level Pipeline Configurations

Pipeline Configurations can be stored in a couple different locations depending on the Job Type.

| Job Type             | Pipeline Configuration Location                                                                                                               |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Pipeline Job         | Either in the Jenkins UI or at the root of a remote source code repository as a file called `pipeline_config.groovy`                          |
| Multi-Branch Project | At the root of the repository in a file named `pipeline_config.groovy` (or to any arbitrary path in the repository as configured by `configurationPath`) in the branch job that was created as part of the Multi-Branch Project |
