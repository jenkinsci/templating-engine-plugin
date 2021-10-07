# Steps

[Library Steps](../library-development/library-steps.md) are technically a Pipeline Primitive, as they're "injected" into the Pipeline Template's runtime during [Pipeline Initialization](../advanced/pipeline-initialization.md). 

Something unique about steps, though, is that they're the only Pipeline Primitive that aren't directly defined in the [Pipeline Configuration](../pipeline-configuration/overview.md).

Instead, the `libraries` block is used to declare which libraries to load.
Each loaded library can then contribute steps.

!!! tip "Learn More"
    Learn more about [Library Steps](../library-development/library-steps.md) or [Libary Development](../library-development/overview.md).