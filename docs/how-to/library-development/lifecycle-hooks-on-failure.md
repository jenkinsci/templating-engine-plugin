# Trigger Lifecycle Hooks On Failure

JTE's [Lifecycle Hooks](../../concepts/library-development/lifecycle-hooks.md) allow library developers to register steps that should execute based on triggers.

These triggers include the start of the pipeline (`@Init`, `@Validate`), before steps (`@BeforeStep`), after steps (`@AfterStep`, `@Notify`), and at the end of the pipeline (`@CleanUp`, `@Notify`).

A common use case is using the conditional execution functionality of lifecycle hooks to trigger the hook when a step or the pipeline fail.

??? warning "Using the `currentBuild` variable won't work"

    Often, developers will try to use the `currentBuild.currentResult` value to achieve this.
    Unfortunately, for the case of pipeline failures the Run's result is not set to FAILURE until the pipeline has completed.
    This is because exceptions thrown may be caught further up the call stack.

## Using `@Notify` for failure

Typically, the `@Notify` hook should be used for pipeline code to send alerts.

The `@Notify` steps runs both after steps and at the end of the pipeline.

To ascertain which event triggered the hook, you can use the `step` field on the `hookContext` variable.

=== "Triggering On Failure of a Step"

    ```groovy
    @Notify({ hookContext.step && hookContext.exceptionThrown })
    void call(){
        println "this will run after any step that throws an exception"
    }
    ```

=== "Triggering On Failure of the Pipeline"

    ```groovy
    @Notify({ hookContext.step == null && hookContext.exceptionThrown })
    void call(){
        println "this will run if the pipeline throws an uncaught exception"
    }
    ```
