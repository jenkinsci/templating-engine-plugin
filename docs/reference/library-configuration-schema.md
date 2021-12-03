# Library Configuration Schema

This page outlines the schema for [Library Configuration Files](../concepts/library-development/library-configuration-file.md).

## Library Configuration Validation

### Schema

``` groovy title="library_config.groovy"
fields{ // (1)
  required{} // (2)
  optional{} // (3)
}
```

1. The `fields{}` block is used to declare what properties are expected in a library configuration.
2. The `required{}` block is used to declare required fields.
3. The `optional{}` block is used to declare optional fields.

Within the `required` and `optional` blocks, list the parameters the library supports in a `parameterName = <Validation Type>` format.

<!-- markdownlint-disable -->
!!! note
    If a library **doesn't** include a library configuration file, then users can supply arbitrary parameters to the library from the Pipeline Configuration.

    If a library **does** include a library configuration file, then users will only be able to supply parameters that are listed within the `required` and `optional` blocks.
    The presence of extraneous parameters will fail the build.
<!-- markdownlint-restore -->

### Supported Validations

The library configuration supports several different validation types for library parameters.

#### Type Validation

Type validation confirms that a library parameter is an instance of a particular type.

The current options for data types to test for are:

* Boolean / boolean
* String
* Integer / int
* Double
* BigDecimal
* Float
* Number
* List
* ArrayList

!!! example "Type Validation Example"
    === "Library Configuration File"
        ``` groovy title="library_config.groovy"
        fields{
          required{
            parameterA = String // (1)
            parameterB = Number // (2)
            parameterC = Boolean // (3)
          }
          optional{
            parameterD = String // (4)
            parameterE = Boolean // (5)
            parameterF = List // (6)
            parameterG = ArrayList // (7)
          }
        }
        ```

        1. ensures that `parameterA` was configured and is an instance of a String
        2. ensures that `parameterB` was configured and is an instance of a Number
        3. ensures that `parameterC` was configured and is an instance of a Boolean
        4. _if_`parameterD` was configured, ensures it's a String
        5. _if_`parameterE` was configured, ensures it's a Boolean
        6. _if_`parameterF` was configured, ensures it's a List
        7. _if_ `parameterG` was configured, ensures it's an ArrayList

#### Enum Validation

Enum validation ensures that a library parameter value is one of the options defined by a list in the library configuration.

!!! example "Enum Validation Example"
    === "Library Configuration File"
        ``` groovy title="library_config.groovy"
        fields{
          required{
            parameterA = [ "a", "b", 11 ] // (1)
          }
        }
        ```

        1. ensures that `parameterA` was configured and is set to either 'a', 'b', or 11

#### Regular Expression Validation

Regular expression validation uses Groovy's [match operator](https://docs.groovy-lang.org/latest/html/documentation/core-operators.html#_match_operator) to determine if the parameter value is matched by the regular expression.

!!! example "Regular Expression Example"
    === "Library Configuration File"
        ``` groovy title="library_config.groovy"
        fields{
          required{
            parameterA = ~/^s.*/ // (1)
          }
        }
        ```

        1. ensures that `parameterA` starts with `s`

### Nested Parameters

Library parameters can be arbitrarily nested within the Pipeline Configuration.

For example, the following Pipeline Configuration would be valid to pass the `example.nestedParameter` parameter to a library named `testing`.

=== "Pipeline Configuration"
    ``` groovy title="pipeline_config.groovy"
    libraries{
      testing{
        example{
          nestedParameter = 11
        }
      }
    }
    ```
=== "Library Configuration"
    ``` groovy title="library_config.groovy"
    fields{
      required{
        example{
          nestedParameter = Number
        }
      }
    }
    ```

!!! tip
    To validate nested library parameters in the library configuration, nest their validation in the same structure within the `required` or `optional` blocks.
