# Application Environments

The Application Environment primitive allows users to encapsulate environmental context. Users can define custom fields from the [Pipeline Configuration](../pipeline-configuration/index.md).

## Defining Application Environments

The `application_environments{}` block is used to define Application Environments.

Within the Application Environments block, environments are defined through nested keys.

For example, the following code block would create `dev` and `test` variables, each referencing an Application Environment object. These variables can be resolved within the Pipeline Template or Library Steps.

``` groovy title="pipeline_config.groovy"
application_environments{
  dev
  test
}
```

## Default Fields

Application Environments can define the optional fields `short_name` and `long_name`.

If not declared, these fields will default to the Application Environment key.

For example:

``` groovy title="pipeline_config.groovy"
application_environments{
  dev
  test{
    short_name = "t"
  }
  staging{
    long_name = "Staging"
  }
  prod{
    short_name = "p"
    long_name = "Production"
  }
}
```

This block defines `dev`, `test`, and `prod` Application Environments. The following table outlines the values of `short_name` and `long_name` for each Application Environment.

| Application Environment | Short Name | Long Name    |
|-------------------------|------------|--------------|
| `dev`                   | "dev"      | "dev"        |
| `test`                  | "t"        | "test"       |
| `staging`               | "staging"  | "Staging"    |
| `prod`                  | "p"        | "Production" |

## Determining Application Environment Order

The order Application Environments are defined within the Pipeline Configuration are used to define `previous` and `next` properties.

For example, defining the following Application Environments

``` groovy title="pipeline_config.groovy"
application_environments{
  dev
  test
  prod
}
```

would result in the following values for `previous` and `next` on each Application Environment:

| Application Environment | previous | next   |
|-------------------------|----------|--------|
| `dev`                   | `null`   | `test` |
| `test`                  | `dev`    | `prod` |
| `prod`                  | `test`   | `null` |

!!! note
    These properties are automatically configured based upon the declaration order within the Pipeline Configuration. If you try to set the `previous` and `next` properties in the environment's definition an exception will be thrown.

## Custom Fields

Application Environments accept custom fields.

These custom fields can be used to capture characteristics about the Application Environment that should be used from the pipeline. Examples include AWS tags to use when querying infrastructure, kubernetes cluster API endpoints, IP addresses, etc.

For example, if there were IP addresses that the pipeline needed to access during execution:

``` groovy title="pipeline_config.groovy"
application_environments{
  dev{
    ip_addresses = [ "1.2.3.4", "1.2.3.5" ]
  }
  test
  prod{
    ip_addresses = [ "1.2.3.6", "1.2.3.7" ]
  }
}
```

This would add an `ip_addresses` property to the `dev` and `prod` objects while `test.ip_addresses` would be `null`.

## Using Application Environments in Deployment Steps

A common pattern is to use Application Environments in conjunction with steps that perform automated deployments. If a library were to contribute a `deploy_to` step that accepted an Application Environment as an input parameter, then a Pipeline Template could be created that leverages these variables.

``` groovy title="Jenkinsfile"
do_some_tests()
deploy_to dev
deploy_to prod
```

A contrived example of a Library Step that follows this pattern is below.

``` groovy title="deploy_to.groovy"
void call(app_env){
  // use the default long_name property to dynamically name the stage
  stage("Deploy to ${app_env.long_name}"){
    // iterate over the environment's ip addresses and print a statement
    app_env.ip_addresses.each{ ip ->
      println "publishing artifact to ${ip}"
    }
  }
}
```

<!-- markdownlint-disable -->
!!! note
    You may have noticed that the template didn't use parenthesis when invoking the `deploy_to` method: `deploy_to dev`.

    This has nothing to do with JTE. Groovy allows you to [omit parentheses](https://groovy-lang.org/style-guide.html#_omitting_parentheses) when passing parameters to a method.
<!-- markdownlint-restore -->