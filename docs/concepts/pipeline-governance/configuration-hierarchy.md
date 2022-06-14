# Configuration Hierarchy

The Configuration Hierarchy is created by configuring these Governance Tiers on Folders[^1] and in the Jenkins Global Configuration[^2].

Pipelines using JTE inherit [Pipeline Configuration](../pipeline-configuration/index.md), [Pipeline Catalogs](../pipeline-templates/pipeline-catalog.md), and [Library Sources](../library-development/library-source.md) from their parent [Governance Tiers](./governance-tier.md) as determined by the hierarchy.

<figure>
  <img src="../configuration-hierarchy.png"/>
  <figcaption>Figure 1. Creating a Configuration Hierarchy</figcaption>
</figure>

[^1]: The [Folders Plugin](https://github.com/jenkinsci/cloudbees-folder-plugin) allows users to define custom taxonomies.
[^2]: You can find the Global Configuration, if you have permission, by navigating to `Manage Jenkins > Configure System`
