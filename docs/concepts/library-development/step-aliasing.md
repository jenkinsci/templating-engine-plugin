# Step Aliasing

Step Aliasing allows library developers to cast the same step to one or more step names at runtime by using the `@StepAlias` annotation.

By default, steps will assume the basename of the files that define them. i.e, a `build.groovy` step file will create a `build` step.

Step Aliasing allows you to change the name (or names) of the step that's going to be created.

This annotation is automatically imported, just like [lifecycle hooks](lifecycle-hooks.md).

## Overview

The use case often arises where a library has multiple steps that are all essentially the same thing.

Step Aliases allow you to write a step one time and invoke it using multiple names.

Steps have access to a [`stepContext` variable](/reference/autowired-variables#stepcontext) to determine the current context of the step, such as the name being used and whether the step is an alias.

## Static Step Aliases

Static step aliases are static lists of strings to cast the step to at runtime.

### Single Static Alias

`@StepAlias` can take a `String` parameter to change the name of the step at runtime.

``` groovy title="generic.groovy"
@StepAlias("build") // (1)
void call(){
    println "running as build!"
}
```

1. `generic.groovy` will be invocable at runtime via `build()`

### Multiple Static Aliases

`@StepAlias` can also accept an array of Strings to alias the step to multiple names.

``` groovy title="generic.groovy"
@StepAlias(["build", "unit_test"]) // (1)
void call(){
    println "running as either build or unit_test!"
}
```

1. `generic.groovy` can be used in the pipeline as either `build()` **or** `unit_test()`

## Dynamic Step Aliases

Sometimes, aliases should themselves be determined at runtime.
This can be accomplished by providing a `dynamic` parameter that should be a `Closure` that returns a string or list of strings.

For example, if a library called `alias` had a step called `generic.groovy` then an `aliases` library parameter could be created:

``` groovy title="pipeline_config.groovy"
libraries{
  alias{
    aliases = ["build", "unit_test"] // (1)
  }
}
```

1. defines a string or list of strings to alias the `generic` step to

This `aliases` parameter can then be consumed within the dynamic step alias closure:

``` groovy title="generic.groovy"
@StepAlias(dynamic = { return config.aliases }) // (1)
void call(){
    println "running as ${stepContext.name}!"
}
```

1. `generic.groovy` can be used in the pipeline as either `build()` **or** `unit_test()`

## Keeping the Original Step

By default, when `@StepAlias` is present in a step file, a step with the original name **won't** be created.

This behavior can be overridden via the `keepOriginal` annotation parameter.

``` groovy title="generic.groovy"
@StepAlias(value = "build", keepOriginal = true) // (1)
void call(){
    println "running as either build() or generic()"
}
```

1. The `keepOriginal` parameter can be used if a step with the original step name should be created

!!! note
    When passing multiple annotation parameters, the default static aliases parameter should be passed as `value`.
