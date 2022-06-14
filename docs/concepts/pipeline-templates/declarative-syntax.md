# Declarative Syntax Support

JTE has supported writing Pipeline Templates in [Declarative Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/) since [version 2.0](https://github.com/jenkinsci/templating-engine-plugin/releases/tag/2.0).

## Some Background

JTE hasn't always supported Declarative Syntax.

With JTE, pipeline authors can create Pipeline Templates that look like a custom DSL.

Take the following Pipeline Template and Pipeline Configuration for example:

=== "Pipeline Template"
    ``` groovy title="Jenkinsfile"
    on_pull_request to: develop, {
      continuous_integration()
    }

    on_merge to: develop, {
      continuous_integration()
      deploy_to dev
      penetration_test()
      integration_test()
      performance_test()
    }

    on_merge to: main, {
      deploy_to prod
    }
    ```
=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    libraries{
      git       // supplies on_pull_request, on_merge
      docker    // supplies build
      npm       // supplies unit test
      sonarqube // supplies static_code_analysis
      helm      // supplies deploy_to
      zap       // supplies penetration_test
      cypress   // supplies integration_test
      jmeter    // supplies performance_test()
    }
    stages{
      continuous_integration{
        build
        unit_test
        static_code_analysis
      }
    }
    application_environments{
      dev
      prod
    }
    keywords{
      develop = ~/^[Dd]ev(elop|elopment|eloper|)$/
      main = ~/^[Mm](ain|aster)$/
    }
    ```

Many users, however, would still prefer to write Pipeline Templates in Declarative Syntax.

## Motivation

> As it's a fully featured programming environment, Scripted Pipeline offers a tremendous amount of flexibility and extensibility to Jenkins users. The Groovy learning-curve isn’t typically desirable for all members of a given team, so Declarative Pipeline was created to offer a simpler and more opinionated syntax for authoring Jenkins Pipeline[^1].

Declarative Syntax offers a simpler and more opinionated way to write Jenkins pipelines.
Users familiar with Declarative Syntax can get started using JTE.

[Pipeline Primitives](../pipeline-primitives/index.md), including [Library Steps](../library-development/library-steps.md), can be resolved from a Pipeline Template written in Declarative Syntax.

## Step Resolution

There is one minor behavioral difference between Pipeline Templates written in Scripted Pipeline Syntax vs Declarative Pipeline Syntax in regard to Step Resolution.

When a Library Step is loaded that overwrites a Jenkins DSL step, such as `sh`, then in Scripted Pipeline Templates the Library Step will take precedence whereas in Declarative Pipeline Templates the original `sh` implementation will take precedence.

The way to bypass this in Declarative Syntax to invoke the Library Step is to invoke it from a `script` block.

!!! tip "Declarative Step Resolution Example"
    === "Declarative Pipeline Syntax"
        Assume a `sh` Library Step has been loaded.
        ``` groovy title="Jenkinsfile"
        pipeline{
          agent any
          stages{
            stage("Example"){
              steps{
                sh "some script" // (1)
                script{
                  sh "some script" // (2)
                }
              }
            }
          }
        }
        ```

        1.  This `sh` call would invoke the original Jenkins DSL Pipeline Step
        2.  This `sh` call, in the `script{}` block, would invoke the loaded JTE Library Step

[^1]: Taken from the [Declarative Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/#compare) documentation.
