# Jenkins Templating Engine

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/templating-engine.svg)](https://plugins.jenkins.io/templating-engine)
[![GitHub Release](https://img.shields.io/github/v/release/jenkinsci/templating-engine-plugin.svg?label=release)](https://github.com/jenkinsci/templating-engine-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/templating-engine.svg?color=blue)](https://plugins.jenkins.io/templating-engine)
[![Gitter](https://badges.gitter.im/jenkinsci/templating-engine-plugin.svg)](https://gitter.im/jenkinsci/templating-engine-plugin)

<table border="0">
  <tr>
  <td>
    <div align="center">
       <img src="docs/jte.png" width="192">
    </div>
  </td>
  <td>

**Table of Contents:**

- [Jenkins Templating Engine](#jenkins-templating-engine)
  - [Overview](#overview)
  - [Learn More](#learn-more)
  - [Participate](#participate)
    - [Adopters](#adopters)
    - [Join the Conversation](#join-the-conversation)
    - [Report a Bug or Request a Feature](#report-a-bug-or-request-a-feature)
    - [Contributions are Welcome!](#contributions-are-welcome)
      - [Documentation](#documentation)
      - [Unit Tests](#unit-tests)
      - [Squash Some Bugs](#squash-some-bugs)
      - [Feature Development](#feature-development)

  </td>
  </tr>
</table>

## Overview

The Jenkins Templating Engine (JTE) is a plugin originally created by [Booz Allen Hamilton](https://www.boozallen.com/) enabling pipeline templating and governance.

JTE brings the [Template Method Design Pattern](https://refactoring.guru/design-patterns/template-method) to Jenkins pipelines.
Users can remove the Jenkinsfile from individual source code repositories in favor of a centralized, tool-agnostic [Pipeline Template](https://jenkinsci.github.io/templating-engine-plugin/2.5/concepts/pipeline-templates/). 
This template provides the structure of the pipeline.

Pipeline functionality is provided by Library Steps.
For example, if the Pipeline Template references a `build()` step then the implementation of `build()` can be deferred to a user-developed library providing that step such as a Maven or Gradle library.

A hierarchical [Pipeline Configuration](https://jenkinsci.github.io/templating-engine-plugin/2.5/concepts/pipeline-configuration/) is used for each individual pipeline to determine which libraries to load (among other things).

## Learn More

There are many resources available to help you get started:

- [Documentation](https://jenkinsci.github.io/templating-engine-plugin)
- **Presentations**
  - [CDF Online Meetup](https://www.youtube.com/watch?v=FYLaoqn0pDE)
  - [Jenkins Online Meetup](https://www.youtube.com/watch?v=pz_kPpb9C1w&feature=youtu.be)
  - [JTE at KubeCon 2019](https://www.youtube.com/watch?v=OClSwxhsspA)
  - [Trusted Software Supply Chains with JTE](https://www.youtube.com/watch?v=TMxUAi3XXOg&list=PLj6h78yzYM2MGKo_LNRA-lhxlNXwiDJDT&index=5&t=0s)
  - [Jenkins World 2018](https://www.youtube.com/watch?v=BM9Vmsh2iMI)
- **Blog Posts**
  - [Introducing the Jenkins Templating Engine](https://jenkins.io/blog/2019/05/09/templating-engine/)

## Participate

There are many ways to get involved. We look forward to hearing from you.

### Join the Conversation

JTE has a [channel](https://gitter.im/jenkinsci/templating-engine-plugin) in the Jenkins community's gitter space.
It's a great place to ask questions or propose ideas for the future of JTE.

### Report a Bug or Request a Feature

Something not quite working right? Have a cool idea for how to make JTE better?
[Open an Issue](https://github.com/jenkinsci/templating-engine-plugin/issues/new/choose) on the JTE repository and let's get the conversation started.

### Contributions Welcome

No contribution is too small - all are appreciated!

Check out the [Contributing Section](https://jenkinsci.github.io/templating-engine-plugin/2.5/contributing/fork-based/) of the documentation to understand how to get started.