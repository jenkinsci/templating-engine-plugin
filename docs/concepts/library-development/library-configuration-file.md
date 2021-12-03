# Library Configuration File

The root of a library can contain an **optional** `library_config.groovy` file that captures metadata about the library.

## Library Parameter Validation

Currently, the library configuration file is only used to validate library configurations.

!!! info "Reference"
    A comprehensive overview of the [library configuration schema](../../reference/library-configuration-schema.md) can be found in the Reference section.

## Advanced Library Validations

For library parameter validations that are more complex than what can be accomplished through the library configuration functionality, library developers can alternatively create a step annotated with the [`@Validate` Lifecycle Hook](lifecycle-hooks.md).

Methods within steps annotated with `@Validate` will execute before the Pipeline Template.

For example, if a library wanted to validate a more complex use case such as ensuring a library parameter named `threshold` was greater than or equal to zero but less than or equal to 100 the following could be implemented:

``` groovy title="threshold_check.groovy"
@Validate // (1)
void call(context){ // (2)
  if(config.threshold < 0 || config.threshold > 100){ // (3)
    error "Library parameter 'threshold' must be within the range of: 0 <= threshold <= 100" // (4)
  }
}
```

1. The `@Validate` annotation marks a method defined within a step to be invoked before template execution.
2. This example defines a `call()` method, but the method name can be any valid Groovy method name.
3. Here, a Groovy if statement is used to validate that the `threshold` parameter fall within a certain range.
4. If the `threshold` variable doesn't meet the criteria, the Jenkins pipeline `error` step is used to fail the build. The `warning` step could also be used if the pipeline user should be notified but the build should continue.

This approach allows library developers to use Groovy to validate arbitrarily complex library parameter constraints.
The method annotated with `@Validate` can be in its own step file or added as an additional method within an existing step file.

!!! note
    The example above assumes that the `threshold` library parameter has been configured as part of the Pipeline Configuration.
    This could be also be validated using Groovy or by combining the functionality of the library configuration file to set the `threshold` parameter as a required field that must be a Number.
