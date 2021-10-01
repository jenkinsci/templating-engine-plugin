# Top-Down Explanation

This explanation is best suited for more experienced Jenkins users that are familiar with Jenkins pipeline's scripted syntax or software developers familiar with software design patterns.

## Overview

To put it simply, the problem JTE is trying to solve is:

> How can organizations stop building pipelines for each application individually?

The answer comes from the idea that within an organization, software development processes can be distilled into a subset of generic workflows.

Regardless of which *tools* are being used, the *process* often says the same. 
Teams are typically going to run unit tests, build a software artifact, scan it, deploy it somewhere, test it some more, and promote that artifact to higher application environments. 
Some teams do more, some teams do less, but it doesn't matter if that process uses `npm`, `sonarqube`, `docker`, and `helm` or `gradle`, `fortify`, and `ansible`; the **process** is the same. 

As depicted in Figure 1, JTE allows you to take that process and represent it as a tool-agnostic pipeline template. 
This abstract pipeline template can then be made concrete by loading pipeline primitives such as steps. 
Which Pipeline Primitives to inject are determined by a Pipeline Configuration. 

<figure>
  <img src="../top-down-1.png"/>
  <figcaption>Figure 1</figcaption>
</figure>

--8<-- "concepts/framework-overview/snippets/design-patterns.md"

## Pipeline Templates

## Pipeline Primitives

## Pipeline Configuration