# Library Structure

## Overview

Each directory within a [Library Source](/docs/concepts/library-development/library-source.md) is a different library that can be loaded via the [Pipeline Configuration](../pipeline-configuration/index.md)

The name of the directory is the library identifier used within the Pipeline Configuration `libraries{}` block when loading the library.

| Path                    | Description                                                                                                                                                   |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `steps/**/*.groovy`     | groovy files under the steps directory will be loaded as steps where the basename of the file will be the name of the function made available in the pipeline |
| `resources/**/*`        | any file under the `resources` directory will be accessible from within Library Steps via the `resource()` step                                               |
| `src/**/*`              | Classes contributed by the library that can be imported from within [Pipeline Templates](../pipeline-templates/index.md) and [steps](library-steps.md)              |
| `library_config.groovy` | the library configuration file                                                                                                                                |

## Example Library Structure

``` bash
exampleLibraryName # (1)
├── steps # (2)
│   └── step1.groovy # (3)
│   └── step2.groovy
├── resources # (4)
│   ├── someResource.txt # (5)
│   └── nested
│       └── anotherResource.json # (6)
├── src # (7)
│   └── example
│       └── Utility.groovy # (8)
└── library_config.groovy # (9)
```

1. This library would be loaded via the `exampleLibraryName` identifier in the `libraries{}` block
2. All steps contributed by the library goes in the `steps` directory
3. An example step. A `step1` step would be added to the pipeline
4. All library resources go in the `resources` directory
5. A root level resource. The contents could be fetched from `step1` or `step2` via `resource("someResource.txt")`
6. A nested resource. The contents could be fetched from `step1` or `step2` via `resource("nested/anotherResource.json")`
7. File paths within the `src` directory must be unique across libraries loaded and will be made available to the Class Loader for both steps and templates
8. A class file containing the `example.Utility` class.
9. The library configuration file
