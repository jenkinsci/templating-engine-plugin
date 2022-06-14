# Overview

In JTE, Pipeline Templates are used to define tool-agnostic *workflows* that can be shared across teams.

Pipeline Templates make use of [Pipeline Primitives](../pipeline-primitives/index.md) to become reusable.

## Just A Jenkinsfile

A Pipeline Template is executed *exactly like a Jenkinsfile*.
In fact, there's almost no functional difference between a Jenkinsfile and a Pipeline Template in JTE.
Regular Jenkins DSL pipeline steps like `node`, `sh`, and `echo` will all work as expected from a Pipeline Template.

What's different, though, is what happens before the template is executed.
During [Pipeline Initialization](../advanced/pipeline-initialization.md), Pipeline Primitives are created and made available to the Pipeline Template.

## Defining Workflows, not Tech Stacks

While creating JTE, it was envisioned that a Pipeline Template represents a *workflow* - not a pipeline for a specific tech stack.

Be careful of the common pitfall of creating an `npm` template for all your NPM applications and a `java` template for all your Java applications.

If you find yourself doing this - compare those templates and see if there would be a way to make converge with more general step names in a common workflow.

One example of having multiple workflows would be if there were two branching strategies used throughout the organization or if JTE was being used for infrastructure pipelines as well as application pipelines.

!!! tip "Do whatever works for you"
    At the end of the day, JTE's goal is to make pipeline development easier at scale.
    Do whatever works best for your organization.

## Creating a contract between the pipeline and teams

One way to think of a Pipeline Template is that it creates an "API contract" or interface between the pipeline and development teams.

The [Pipeline Configuration](../pipeline-configuration/index.md) is what "hydrates" the template to make it concrete by declaring which Pipeline Primitives should be loaded.

## Learn More

| Page                                                            | Description                                                                    |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------|
| [Pipeline Catalog](./pipeline-catalog.md)                       | Learn about how to build a catalog of Pipeline Templates teams can choose from |
| [Declarative Syntax Support](./declarative-syntax.md)           | Learn how to write templates using Jenkins Declarative Syntax                  |
