# Governance Tier

Governance Tiers are nodes in the [Configuration Hierarchy](../concepts/pipeline-governance/configuration-hierarchy.md).

This page explain the options to configure a Governance Tier.

## Overview

Governance Tiers can store three important things: a [Pipeline Catalog](../concepts/pipeline-templates/pipeline-catalog.md), a [Pipeline Configuration](../concepts/pipeline-configuration/index.md), and a list of [Library Sources](../concepts/library-development/library-source.md).

The configuration for Library Sources stands alone.
The configuration for the Pipeline Catalog and Pipeline Configuration are grouped together.

## Library Sources

Governance Tiers can configure a list of [Library Sources](../concepts/library-development/library-source.md).

When adding a Library Source, there will be a dropdown to determine the type of Library Provider.
A Library Provider is a retrieval mechanism for libraries.

JTE packages two types of Library Providers as part of the plugin: "From SCM" and "From Plugin."

!!! note
    Users will only see the "From Plugin" option available in the dropdown if a plugin has been installed that's capable of providing libraries.

The ordering of Library Sources in the list impacts [Library Resolution](../concepts/pipeline-governance/library-resolution.md).

### From Remote Repository

When configuring a Library Source that fetches from a remote repository, users can configure the type of source code repository as well as the configuration base directory.

The configuration base directory is the path within the remote repository where the libraries can be found.

Each subdirectory within the configuration base directory will be treated as a library.

!!! info
    Refer to the [Library Structure](../concepts/library-development/library-structure.md) for how to organize files within a library directory.

<!-- ### From A Plugin -->
<!-- TODO -->
<!-- really ought to write a gradle plugin for this.. -->

## Pipeline Catalog

### Default Pipeline Template

| Governance Tier Type     | Location of Default Pipeline Template                           |
|--------------------------|-----------------------------------------------------------------|
| From a Remote Repository | a `Jenkinsfile` at the root of the configuration base directory |
| From the Jenkins Console | a dedicated text box labeled 'Default Template'                 |

### Named Pipeline Templates

| Governance Tier Type     | Location of Named Pipeline Templates                                                                         |
|--------------------------|--------------------------------------------------------------------------------------------------------------|
| From a Remote Repository | groovy files within a `pipeline_templates` directory located at the root of the configuration base directory |
| From the Jenkins Console | a list of named templates can be added directly in the Jenkins Console                                       |

## Pipeline Configuration

| Governance Tier Type     | Location of the Pipeline Configuration                                     |
|--------------------------|----------------------------------------------------------------------------|
| From a Remote Repository | a `pipeline_config.groovy` at the root of the configuration base directory |
| From the Jenkins Console | a text field labeled Pipeline Configuration                                |
