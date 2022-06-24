# Pipeline Configuration Schema

## Overview

Check out the [Pipeline Configuration](../concepts/pipeline-configuration/index.md) page for an explanation of the Pipeline Configuration's purpose and syntax.

## Root-Level Blocks

### `jte`

The `jte{}` block of the Pipeline Configuration is reserved for fields that change framework behavior.

| key                          | description                                                                                                                                                                                                                                                                                                                        | type    | default |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| `allow_scm_jenkinsfile`      | Determines whether a Jenkinsfile in a source code repository will be used when determining the Pipeline Template. Refer to [Pipeline Template Selection](../concepts/pipeline-governance/pipeline-template-selection.md) for more information.                                                                                     | Boolean | `True`  |
| `permissive_initialization`  | Determine whether to fail the build during [pipeline initialization](../concepts/advanced/pipeline-initialization.md) if multiple [Pipeline Primitives](../concepts/pipeline-primitives/index.md) with conflicting names are loaded. Setting to `True` will allow multiple Pipeline Primitives with the same name to be loaded. | Boolean | `False` |
| `pipeline_template`          | Specify a named template from the [Pipeline Catalog](../concepts/pipeline-templates/pipeline-catalog.md) to use.                                                                                                                                                                                                                   | String  | `null`  |
| `reverse_library_resolution` | Determine whether to reverse the order that [Library Sources](../concepts/library-development/library-source.md) are queried for a library. Refer to [Library Resolution](../concepts/pipeline-governance/library-resolution.md) for more information.                                                                             | Boolean | `False` |

!!! example "Example JTE Block"
    ``` groovy title="pipeline_config.groovy"
    jte{
      allow_scm_jenkinsfile = False
      permissive_initialization = True
      pipeline_template = "my-named-template.groovy"
      reverse_library_resolution = True
    }
    ```

### `template_methods`

The `template_methods{}` block is used to define the names of steps to create a no-op placeholder for if a library doesn't provide the step's implementation.

Refer to [Placeholder Steps](../concepts/pipeline-primitives/steps.md#placeholder-steps) for more information.

!!! example "Example `template_methods` block"
    ``` groovy title="pipeline_config.groovy"
    template_methods{
      unit_test
      build
      deploy_to
    }
    ```

### `libraries`

The `libraries{}` block determines which libraries to load. The block names within `libraries` must reference a library within a configured [Library Source](../concepts/library-development/library-source.md) available to the job.

Refer to the [Library Development Overview](../concepts/library-development/index.md) for more information.

!!! example "Example libraries block"
    ``` groovy title="pipeline_config.groovy"
    libraries{
      library_A{
        param1 = "foo"
        param2 = "bar"
        ...
      }
      ...
    }
    ```

### `stages`

The `stages{}` block is used to define [Stages](../concepts/pipeline-primitives/stages.md).

!!! example "Example stages block"
    ``` groovy title="pipeline_config.groovy"
    stages{
      stage_name{
        step1
        step2
        ...
      }
      ...
    }
    ```

### `application_environments`

The `application_environments{}` block is used to define [Application Environments](../concepts/pipeline-primitives/application-environments.md).

!!! example "Example `application_environments` block"
    ``` groovy title="pipeline_config.groovy"
    application_environments{
      dev{
        long_name = "Development"
      }
      test
      prod
    }
    ```

### `keywords`

The `keywords{}` block is used to define [Keywords](../concepts/pipeline-primitives/keywords.md).

!!! example "Example keywords block"
    ``` groovy title="pipeline_config.groovy"
    keywords{
      main = ~/^[Mm](ain|aster)$/
      globals{
        foo = "bar"
      }
    }
    ```
