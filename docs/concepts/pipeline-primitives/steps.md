# Steps

[Pipeline Templates](../pipeline-templates/index.md) represent generic software delivery workflows.

Pipeline Templates use Steps to represent tasks in that workflow.

!!! tip "Best Practice"
    It is recommended that Steps are named generically. For example, rather than `npm_build()` the Step should be named `build()`.

    By naming Steps generically, multiple libraries can implement the same Step.
    This allows teams to share the same Pipeline Template by loading different libraries via the [Pipeline Configuration](../pipeline-configuration/index.md). 

## Placeholder Steps

Users can create Placeholder Steps that do nothing and serve as a no-op[^1] Step.

The primary purpose of these Placeholder Steps is to avoid a `NoSuchMethodError` being thrown when the Pipeline Template attempts to invoke a Step that hasn't been contributed by a library.

To define Placeholder Steps, use the `template_methods{}` block.

!!! example "Example: Defining Placeholder Steps"
    In the following example, a Pipeline Template expects to invoke a `unit_test()` and a `build()` step.

    It's expected that users will declare in their Pipeline Configurations libraries that implement these steps. 

    In case that's not true, the `template_methods{}` block has been configured to substitute Placeholder Steps to avoid an exception being thrown. 
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        unit_test()
        build()
        ```
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        template_methods{
          unit_test
          build
        }
        ```
    === "Build Log"
        ``` text
        [Pipeline] Start of Pipeline
        [JTE][Step - null/unit_test.call()]
        [Pipeline] echo
        Step unit_test is not implemented.
        [JTE][Step - null/build.call()]
        [Pipeline] echo
        Step build is not implemented.
        [Pipeline] End of Pipeline
        Finished: SUCCESS
        ```

## Library Steps

[Library Steps](../library-development/library-steps.md) are contributed by libraries.

Users define in the [Pipeline Configuration](../pipeline-configuration/index.md) which libraries to load, if any.

!!! info "Learn More"
    Learn more about how to create libraries over in the [Library Development](../library-development/index.md) section.

[^1]: [No Operation](https://en.wikipedia.org/wiki/NOP_(code)): a command that does nothing.
