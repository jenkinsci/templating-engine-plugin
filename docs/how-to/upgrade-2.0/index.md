# 2.0 Upgrade Guide

This page is going to help walk you through the breaking changes associated with 2.0.

## Library File Structure

To support [Library Resources](../../concepts/library-development/library-resources.md), the file structure of libraries has been reorganized.

=== "Pre-2.0"
    ```
    .
    ├── libraryA // libraries are directories
    │  ├── library_config.groovy // library config at root
    │  └── someOtherStep.groovy // step files at root of library directory
    └── libraryB
      ├── library_config.groovy
      └── someOtherStep.groovy
    ```
=== "Post-2.0"
    ```
    .
    ├── libraryA // libraries are still directories
    │  ├── library_config.groovy // library config still at root
    │  ├── resources // new resources directory!
    │  │  └── someScript.sh
    │  └── steps // steps are now in a steps directory
    │     └── someOtherStep.groovy
    └── libraryB
      ├── library_config.groovy
      ├── resources
      │  └── someData.json
      └── steps
          └── someOtherStep.groovy
    ```

## `@override` & `@merge` annotations

Previously, the ability to govern pipeline configuration changes were confined to the block-level - with no way to govern individual fields.
To address this, JTE has pivoted from the flags `merge=true` & `override=true` to the *annotations* `@merge` & `@override`.
These annotations can be placed on individual fields within a block, enabling field-level governance.

=== "Pre-2.0"
    ``` groovy
    someBlock{
      merge = true // future configs can add fields to this block
      my_governed_field = "some value"// cannot be modified
    }
    anotherBlock{
      override = true // entire block can be overridden. no way to only override a field in a block.
      may_not_be_changed = true
      default_value_may_be_changed = true
    }
    ```
=== "Post-2.0"
    ``` groovy
    @merge someBlock{ // future configs can add fields to this block
      my_governed_field = "some value"
    }
    anotherBlock{ // future configs can't add fields to this block
      may_not_be_changed = true // not modifiable
      @override default_value = true  // may be overridden
    }
    ```

## Top level pipeline configuration values and the `jte{}` block

Previously, there were top level configuration values like `allow_scm_jenkinsfile` and `pipeline_template`.
These values are now in the `jte` block in the pipeline_config

=== "Pre-2.0"
    ``` groovy title="pipeline_config.groovy"
    allow_scm_jenkinsfile = false
    pipeline_template = "my_template"
    libraries{} // just here to show the relation to the root
    ```

=== "Post-2.0"
    ``` groovy title="pipeline_config.groovy"
    jte{
      allow_scm_jenkinsfile = false
      pipeline_template = "my_template"
    }
    libraries{} // just here to show relation to the root
    ```

## Lifecycle Hook: `hookContext`

JTE provides some *syntactic sugar* by means of autowiring variables to library steps to simplify library development.

Previously, library steps that implemented lifecycle hooks were required to accept a method parameter to accept the hook context.
This parameter was typically called `context` but could be called anything.

=== "Pre-2.0"
    ``` groovy
    @AfterStep({ context.step == "build" }) // variable called context
    void call(context){ // hooks required to accept a method parameter
      println "running after the ${context.step} step"
    }
    ```
=== "Post-2.0"
    ``` groovy
    @AfterStep({ hookContext.step == "build"}) // variable called hookContext
    void call(){ // no method parameter required
      println "running after the ${hookContext.step} step" // hookContext variable autowired
    }
    ```

## Configuration Changes

JTE 2.0 resulted in significant refactoring of the codebase and underlying package structure.

### Global Configurations

There have been updates to the underlying class structure of the Global Governance Tier configured in `Manage Jenkins > Configure System > Jenkins Templating Engine`.
This will impact the Jenkins Configuration as Code (JCasC) YAML schema used to configure JTE.

!!! tip
    It's recommended to configure the Global Governance Tier manually the way you require and exporting the JCasC YAML to see the schema required to automate configuring JTE.

### Job Configurations

There have been updates to the underlying package and class structure for JTE as a whole as well as feature development for ad hoc pipeline jobs.
This impacts Job DSL scripts used to configure jobs utilizing JTE.

JTE also now supports fetching the Pipeline Configuration and Pipeline Template for a one-off pipeline job, which results in some changes to the structure of Job DSL for ad hoc pipeline jobs.

!!! tip
    Job DSL supports [Dynamic DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki/Dynamic-DSL) which means that Job DSL supports the Jenkins Templating Engine settings.
    It's recommended to utilize the Job DSL API Viewer on your Jenkins Instance once JTE 2.0 has been installed to see how to configure JTE settings.
