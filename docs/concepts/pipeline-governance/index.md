# Overview

One of the [Key Benefits](../framework-overview/key-benefits.md) of using JTE is the governance it can bring to software delivery.

JTE achieves this governance by creating a [Configuration Hierarchy](./configuration-hierarchy.md) using Jenkins global settings and Folder properties.

The nodes of this hierarchy, called [Governance Tiers](./governance-tier.md), store [Pipeline Configurations](../pipeline-configuration/index.md), a [Pipeline Catalog](../pipeline-templates/pipeline-catalog.md), and [Library Sources](../library-development/library-source.md).

Teams can create arbitrarily complex governance hierarchies simply by organizing jobs in Jenkins into the appropriate Folders.

## Learn More

| Page                                                            | Description                                                                                                                                                    |
|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Configuration Hierarchy](./configuration-hierarchy.md)         | Learn how to set up hierarchical Pipeline Configurations                                                                                                       |
| [Governance Tier](./governance-tier.md)                         | Learn how to configure a node of the Configuration Hierarchy                                                                                                   |
| [Pipeline Template Selection](./pipeline-template-selection.md) | Learn how JTE determines which Pipeline Template to use for a given Pipeline Run                                                                               |
| [Library Resolution](./library-resolution.md)                   | Learn how JTE choose which library to load when there are multiple choices within the available [Library Sources](../library-development/library-resources.md) |
| [Governance Tier](./governance-tier.md)                         | Learn how to configure a [Governance Tier](../pipeline-governance/governance-tier.md)                                                                          |
