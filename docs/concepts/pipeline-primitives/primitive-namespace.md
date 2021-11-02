# Primitive Namespace

The Pipeline Primitive Namespace is an [Autowired Variable](../../reference/autowired-variables.md) called `jte` that's accessible everywhere.

It can be used to access all the loaded Pipeline Primitives for a given Pipeline Run.

## Accessing Libraries and Steps

If libraries were loaded, the `jte` variable will have a `libraries` property that stores the library's steps.

!!! example "Invoking a Step Using The Primitive Namespace"
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        libraries{
          npm // contributes a build() step
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        jte.libraries.npm.build()
        ```

## Accessing Keywords

If [Keywords](./keywords.md) were defined, the `jte` variable will have a `keywords` property that stores the Keywords.

!!! example "Accessing Keywords"
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        keywords{
          foo = "bar"
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        assert jte.keywords.foo == "bar"
        ```

## Accessing Application Environments

If [Application Environments](./application-environments.md) were defined, the `jte` variable will have an `application_environments` property that stores the Application Environments.

!!! example "Accessing Application Environments"
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        application_environments{
          dev{
            ip = "1.1.1.1"
          }
          prod{
            ip = "2.2.2.2"
          }
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        assert jte.application_environments.dev.ip == "1.1.1.1"
        assert jte.application_environments.prod.ip == "2.2.2.2"
        ```

## Accessing Stages

If [Stages](./stages.md) were defined, the `jte` variable will have an `stages` property that stores the Stages.

!!! example "Accessing Application Environments"
    === "Pipeline Configuration"
        ``` groovy title="pipeline_config.groovy"
        libraries{
          npm // contributes unit_test, build
        }
        stages{
          continuous_integration{
            unit_test
            build
          }
        }
        ```
    === "Pipeline Template"
        ``` groovy title="Jenkinsfile"
        jte.stages.continuous_integration()
        ```
