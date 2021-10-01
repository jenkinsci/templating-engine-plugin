# Getting Started With Step Aliasing

## What You'll Learn

[Step Aliasing](../../concepts/library-development/step-aliasing.md) makes it possible for the same [library step](../../concepts/library-development/library-steps.md) to be invoked by multiple names.

## Tutorial Prerequisites

This tutorial assumes a Jenkins instance is available that is already configured with a [library source](../../concepts/library-development/library-source.md). 

If this is not the case, complete the [local development tutorial](../local-development/index.md) before proceeding.

## Create the Library

## Create a Step

make sure the step uses stepContext.name

## Add a Static Alias

## Add Multiple Static Aliases

## Add Dynamic Aliases

## Getting Fancy

Libraries that interact with build tools such as `npm`, `maven`, or `gradle` would often benefit from Step Aliasing.

As a simple example, let's take an `npm` example.

.pipeline configuration
[source, groovy]
----
libraries{
  npm{
    phases{
        build{
            script = "package"
        }
        unit_test{
            script = "test"
        }
    }
  }
}
----

.npm_invoke.groovy
[source, groovy]
----
@StepAlias(dynamic = { return config.phases.keySet() })
void call(){
  stage("NPM: ${stepContext.name}"){
    // determine phase configuration based on step alias
    def phaseConfig = config.phases[stepContext.name]

    // ensure package.json has the phase script target
    def packageJSON = readJSON "package.json"
    if(!packageJSON.scripts.containsKey(phaseConfig.script)){
        error "package.json does not contain script ${phaseConfig.script}"
    }

    // run npm script
    sh "npm run ${phaseConfig.script}"
  }
}
----

[NOTE]
====
This example is intentionally not production ready.

Its intent is to just show how Step Aliases could be used in a real library.
====

Previously, when writing libraries such as this, common logic around tool versioning, error checking, etc would have to be either duplicated across multiple libraries.
Sometimes, a generic invoking step would be created and accept the "phase" as a method argument from other library steps.

Step Aliasing simplifies these types of setups.
