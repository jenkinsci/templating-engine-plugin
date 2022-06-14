# Keywords

Keywords let users declare variables from the [Pipeline Configuration](../pipeline-configuration/index.md) that can be resolved from the [Pipeline Template](../pipeline-templates/index.md) or [Library Steps](./steps.md).

## Defining Keywords

Keywords are defined via the `keywords{}` block in the Pipeline Configuration.

For example,

``` groovy title="pipeline_config.groovy"
keywords{
  foo = "bar" 
}
```

would then result in a `foo` variable with the value `"bar"`.

## Use Cases

### Global Variables

Keywords can be used to define a `globals` variable accessible from the Pipeline Template and Library Steps.

``` groovy title="pipeline_config.groovy"
keywords{
  globals{
    one = 1
    two = 2
  }
}
```

### Regular Expressions for Conditionals

Keywords can be used to define regular expressions corresponding to common branch names for use from the Pipeline Template to keep the template easy to read.  

=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    keywords{
      main = ~/^[mM]a(in|ster)$/
      develop = ~/^[Dd]evelop(ment|er|)$/
    }
    ```
=== "Pipeline Template"
    ``` groovy title="Jenkinsfile"
    on_pull_request to: develop, {
      /*
        execute on a PR to branches matching the regular expression
        defined by the "develop" keyword
      */
    }
    on_pull_request to: main, from: develop, {
      /*
        execute on a PR from a branch matching the regular expression
        defined by the "develop" keyword to a branch matching the regular
        expression defined by the "main" keyword
      */
    }
    on_merge to: main, {
      /*
        execute when a PR is merged into a branch that matches the regular
        expression defined by the "main" keyword
      */
    }
    ```

!!! note
    The steps in this example (`on_pull_request` and `on_merge`) aren't a part of the Jenkins Templating Engine.
