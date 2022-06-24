---
hide:
  - toc
---

# Developer Docs

## Local Development Environment

| Tool                                         | Purpose                                                                       |
|----------------------------------------------|-------------------------------------------------------------------------------|
| [Gradle]( https://gradle.org)                | Used to run unit tests, package the JPI, and publish the plugin               |
| [Just](https://github.com/casey/just)        | A task runner. Used here to automate common commands used during development. |
| [Docker](https://www.docker.com/get-started) | Used to build the documentation for local preview                             |

## Topics

| Topic                                                         | Description                                                    |
|---------------------------------------------------------------|----------------------------------------------------------------|
| [Pipeline Initialization](./pipeline-initialization/index.md) | The bulk of JTE - covers what JTE does before a Pipeline Run |
| [Jenkins Configuration](./jenkins-config/index.md)            | Explains how JTE is configured through the Jenkins UI          |
| [Testing](./testing/index.md)                                 | Covers how unit testing is done for JTE                        |
