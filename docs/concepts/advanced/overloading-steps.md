# Overloading Steps

Function Overloading[^1] is when there are multiple methods with the same name and different implementation.

## Conflicting Library Steps

To allow multiple libraries to contribute the same step, the [Pipeline Configuration](../pipeline-configuration/index.md) must have `jte.permissive_initialization` set to true.

If multiple libraries *do* contribute the same step, the step will no longer be able to be invoked by its `short_name`.
Instead, overloaded steps must be accessed using the [Pipeline Primitive Namespace](../pipeline-primitives/primitive-namespace.md).

!!! example "Overloaded Library Steps"
    The following example assumes a `gradle` and `npm` library are available that both contribute a `build()` step.

    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        jte{
          permissive_initialization = true // pipeline will fail if not set
        }
        libraries{
          npm    // contributes build()
          gradle // contributes build()
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        // build() <-- would fail because step is overloaded
        jte.libraries.npm.build() 
        jte.libraries.gradle.build()
        ```

!!! danger "Default Mode is to Fail"
    By default, if two libraries are loaded that contribute the same step then the Pipeline Run will fail during [Pipeline Initialization](./pipeline-initialization.md).

    This behavior is modified by setting `jte.permissive_initialization` to `True`.

## Library Steps Overriding Jenkins Pipeline Steps

Jenkins Pipeline DSL steps can be overridden by [Library Steps](../library-development/library-steps.md).

If a Library Step has the same name as a Jenkins Pipeline DSL step, such as `node` or `build`, the Library Step will take precedence.

To invoke the original Jenkins Pipeline DSL Step, use the [`steps` Global Variable](../../reference/autowired-variables.md#steps).

!!! note "Declarative Syntax"
    This isn't true for Declarative Syntax.
    Check out the [Declarative Syntax](../pipeline-templates/declarative-syntax.md#step-resolution) page to learn more.

!!! example "Use Case: Overriding `node`"
    If a library were to contribute a Library Step called `node`, then it would override the `node` step used in Jenkins Scripted Pipelines.

    The following example shows how to override the default `node` step to augment its functionality.

    === "`node.groovy`"
        ``` groovy title="node.groovy"
        // support the original node interface
        void call(String label = null, Closure body){
            if(label){
              steps.node(label, body) // steps var used to access original "node" implementation
            } else {
              steps.node(body)
            }
        }

        // support new functionality
        void call(Map args = [:], Closure body){
          if(args.containsKey("container")){
            docker.image(args.container).inside(body)
          }
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        // assume the Pipeline Configuration loaded the library contributing node.groovy
        node{ println "hi" }
        node("my-label"){ println "hi" }
        node(container: "maven"){ sh "mvn -v" } // custom functionality
        ```

[^1]: [Function Overloading](https://en.wikipedia.org/wiki/Function_overloading)
