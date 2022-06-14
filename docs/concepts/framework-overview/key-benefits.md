# Key Benefits

Here's a distilled explanation of why you should use JTE.

1. JTE is a pipeline development *framework* for creating tool-agnostic, templated workflows that can be shared across teams creating applications with different technologies.
2. This approach separates the business logic ([*Pipeline Template*](../pipeline-templates/index.md)) from the technical implementation ([*Pipeline Primitives*](../pipeline-primitives/index.md)) allowing teams to configure their pipelines rather than build them from scratch.

!!! success "Business Value"
    === "Organizational Governance"
        The elements of [Pipeline Governance](../pipeline-governance/index.md) in JTE allow organizations to scale DevSecOps and have assurances that required security gates are being performed.
    === "Optimize Pipeline Code Reuse"
        The plug-and-play nature of [Pipeline Primitives](../pipeline-primitives/index.md) helps keep [Pipeline Templates](../pipeline-templates/index.md) DRY.
    === "Simplify Pipeline Maintainability"
        When managing more than a couple pipelines, it's simpler to manage a set of reusable Pipeline Templates in a [Pipeline Catalog](../pipeline-templates/pipeline-catalog.md) with modularized [Pipeline Libraries](./../library-development/index.md) than it is to copy and paste a `Jenkinsfile` into every repository and tweak it for the new application.
