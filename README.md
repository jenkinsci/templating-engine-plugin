# Jenkins Templating Engine

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/templating-engine.svg)](https://plugins.jenkins.io/templating-engine)
[![GitHub Release](https://img.shields.io/github/v/release/jenkinsci/templating-engine-plugin.svg?label=release)](https://github.com/jenkinsci/templating-engine-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/templating-engine.svg?color=blue)](https://plugins.jenkins.io/templating-engine)
[![Gitter](https://badges.gitter.im/jenkinsci/templating-engine-plugin.svg)](https://gitter.im/jenkinsci/templating-engine-plugin)

<div align="center">
   <img src="docs/modules/ROOT/images/jte.png" width="192">
</div>

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

## Overview

The Jenkins Templating Engine (JTE) is a plugin originally created by [Booz Allen Hamilton](https://www.boozallen.com/) enabling pipeline templating and governance.  

Different teams are going to use different tools, but the flow of the pipeline is typically consistent. Having to maintain a Jenkinsfile in every source code repository introduces real challenges when scaling a DevOps pipeline across an organization: 

1. *Time*:  Typical Jenkins Shared Libraries help to consolidate common pipeline code, but software teams should focus on where they provide business value: building their application.  Jenkins can have a nontrivial learning curve and there's a desire to achieve economies of scale within an organization. 
2. *Governance*: Having a Jenkinsfile in every repository makes it difficult, if not impossible, to ensure that every team is following an agreed upon business process to get code from developer's laptops out to users in proudction.  Furthermore, developer's have access to this Jenkinsfile and could have permission to skip required quality or security gates. 
3. *Maintainability*:  Incorporating lessons learned into the pipeline over time becomes untenable when there are multiple pipeline definitions - it requires opening a Pull Request across each source code repository. 

JTE aims to remove this friction by pulling the Jenkinsfile out of individual source code repositories and instead, creating tool-agnostic pipeline templates that can be reused across multiple teams. 

<div align="center">
   <img src="docs/modules/ROOT/images/jte.gif">
</div>

For the academic coders out there, one way to describe the Jenkins Templating Engine is an implementation of the Template Method Design Pattern for Jenkins pipelines.  JTE essentially separates the business logic of a pipeline from the technical implementation.  

Templates define a workflow by calling *steps*.  Steps are contributed by libraries. Instead of a Jenkinsfile in each source code repository, teams can now inherit a pipeline template (or choose a template from a pipeline catalog) and then *configure* the pipeline through a configuration file that specifies what libraries to load.  Based on what libraries are loaded, the same pipeline can be used to support an arbitrary combination of tools.  

If your pipeline template is setup to build, test, scan, deploy then it doesn't matter if you're using Docker, Maven, SonarQube, and Helm or NPM, Fortify, and S3: both pipelines can now be executed via the same pipeline template. 

## Learn More

There are many resources available to help you get started: 

- [Documentation](https://boozallen.github.io/sdp-docs/jte/1.7.1/index.html)
- **Learning Labs**
  - [Local Development (getting up and running with JTE)](https://boozallen.github.io/sdp-docs/learning-labs/1/local-development/index.html)
  - [JTE: The Basics](https://boozallen.github.io/sdp-docs/learning-labs/1/jte-the-basics/index.html)
  - [JTE: Learn the Primitives](https://boozallen.github.io/sdp-docs/learning-labs/1/jte-primitives/index.html)
  - [JTE: Advanced Features](https://boozallen.github.io/sdp-docs/learning-labs/1/jte-advanced-features/index.html)
- **Presentations**
  - [Jenkins Online Meetup](https://www.youtube.com/watch?v=pz_kPpb9C1w&feature=youtu.be)
  - [JTE at KubeCon 2019](https://www.youtube.com/watch?v=OClSwxhsspA)
  - [Trusted Software Supply Chains with JTE](https://www.youtube.com/watch?v=TMxUAi3XXOg&list=PLj6h78yzYM2MGKo_LNRA-lhxlNXwiDJDT&index=5&t=0s)
  - [Jenkins World 2018](https://www.youtube.com/watch?v=BM9Vmsh2iMI)
- **Blog Posts**
  - [Introducing the Jenkins Templating Engine](https://jenkins.io/blog/2019/05/09/templating-engine/)


## Participate 

There are many ways to get involved. We look forward to hearing from you. 

### Adopters 

If you're using the Jenkins Templating Engine we would love to hear about it!  Feel free to open a Pull Request with an update to our [ADOPTERS.adoc](https://github.com/jenkinsci/templating-engine-plugin/blob/master/docs/modules/ROOT/pages/ADOPTERS.adoc) file and your use case will be displayed on the [adopters page](https://boozallen.github.io/sdp-docs/jte/1.7.1/ADOPTERS.html). 

### Join the Conversation

JTE has a [channel](https://gitter.im/jenkinsci/templating-engine-plugin) in the Jenkins community's gitter space.  It's a great place to ask questions or propose ideas for the future of JTE. 

### Report a Bug or Request a Feature 

Something not quite working right?  Have a cool idea for how to make JTE better?  Open an [Issue](https://github.com/jenkinsci/templating-engine-plugin/issues) on the JTE repo and let's get the conversation started. 

### Contributions are Welcome! 

No contribution is too small and are greatly appreciated.

#### Documentation 

Documentation is crucial to the health of JTE as a framework.  Please feel free to update the documentation to resolve typos, make an explanation more clear, or add a new page to explain a particular pattern or concept.

JTE is the core engine that powers Booz Allen's open source DevSecOps capability, the Solutions Delivery Platform.  To facilitate the aggregation of documentation, we leverage a documentation framework called [Antora](https://antora.org/). Documentation for Antora is written in [AsciiDoc](https://asciidoctor.org/). The documentation can be found in the ``docs`` directory.  To view your changes locally, run ``make docs`` and then open ``docs/html/index.html``. 

#### Unit Tests

The Jenkins Templating Engine is written in [Groovy](https://groovy-lang.org/) and Unit Tests for the Jenkins Templating Engine are written using [Spock](http://spockframework.org/spock/docs/1.3/all_in_one.html). 

#### Squash Some Bugs 

Feel free to open a Pull Request that addresses one of the bugs outlined in an [Issue](https://github.com/jenkinsci/templating-engine-plugin/issues)! 

#### Feature Development 

New features are welcome. JTE strives to be an unopinionated framework for creating tool-agnostic pipeline templates.  Some features are great ideas, but belong in a separate plugin or can be implemented through libraries.  Because of this, it would be best to open an Issue to discuss the feature first so we can have a conversation about it to see if there's already a way to achieve the same functionality without bringing it into JTE's core. 
