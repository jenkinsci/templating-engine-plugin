# Top-Down Explanation

Welcome :wave:!

This explanation is best suited for more experienced Jenkins users that are familiar with Jenkins pipeline's scripted syntax or software developers familiar with software design patterns.

## Overview

To put it simply, the problem JTE is trying to solve is:

> How can organizations stop building pipelines for each application individually?

The answer comes from the idea that within an organization, software development processes can be distilled into a subset of generic workflows.

Regardless of which *tools* are being used, the *process* often says the same.
Teams are typically going to run unit tests, build a software artifact, scan it, deploy it somewhere, test it some more, and promote that artifact to higher Application Environments.
Some teams do more, some teams do less, but it doesn't matter if that process uses `npm`, `sonarqube`, `docker`, and `helm` or `gradle`, `fortify`, and `ansible`; the **process** is the same.

As depicted in Figure 1, JTE allows you to take that process and represent it as a tool-agnostic Pipeline Template.
This abstract Pipeline Template can then be made concrete by loading Pipeline Primitives such as steps.
Which Pipeline Primitives to inject are determined by a Pipeline Configuration.

<figure>
  <img src="../top-down-1.png"/>
  <figcaption>Figure 1</figcaption>
</figure>

--8<-- "concepts/framework-overview/snippets/design-patterns.md"

## Pipeline Templates

A [Pipeline Template](../pipeline-templates/index.md) is nothing more than a Jenkinsfile with some *stuff* added at runtime.

Everything that can be done in a Jenkinsfile can be done in a Pipeline Template.

The only thing that makes a template a template is the use of Pipeline Primitives such that a single template can be used for multiple teams and multiple tools.

<figure>
  <img src="../jte.gif"/>
  <figcaption>A visualization of a Pipeline Template with steps being swapped in an out</figcaption>
</figure>

## Pipeline Primitives

[Pipeline Primitives](../pipeline-primitives/index.md) are contributed by the JTE framework and help make templates reusable.

Pipeline Templates will typically make use of identically named Pipeline Primitives, such as **step** called `build()`, to become reusable.

## Pipeline Configuration

The [Pipeline Configuration](../pipeline-configuration/index.md) is where teams declare which Pipeline Primitives should be injected into the Pipeline Template for their application(s).
