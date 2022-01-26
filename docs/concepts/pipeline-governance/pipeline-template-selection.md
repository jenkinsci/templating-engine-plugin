# Pipeline Template Selection

Pipeline Template Selection is the name of the process that determines which [Pipeline Template](../pipeline-templates/overview.md) to use for a given Pipeline Run.

Figure 1 visualizes this process in a flow chart.

<figure>
  <img src="../pipeline-template-selection.png"/>
  <figcaption>Figure 1. Pipeline Template Selection Flow Chart</figcaption>
</figure>

## Job Type Matters

### Ad Hoc Pipeline Jobs

JTE treats ad hoc Pipeline Jobs a little differently than Pipeline Jobs that have been created by a MultiBranch Project.

For Pipeline Jobs, if a Pipeline Template has been configured, it will be used.
If not, JTE will follow the flow described throughout the rest of this document.

!!! note "Rationale"
    The rationale for using the configured template without falling back to the rest of the Pipeline Template Selection process is that if a user has permissions to create and configure their own Jenkins job, Pipeline Governance is already gone.

### MultiBranch Project Pipeline Jobs

For MultiBranch Project Pipeline Jobs, if the source code repository has a `Jenkinsfile` at the root (or at any arbitrary path in the repository as configured by `scriptPath`) **and** `jte.allow_scm_jenkinsfile` is set to `True`, then the repository `Jenkinsfile` will be used as the Pipeline Template.

!!! important "Disabling Repository Jenkinsfiles"
    It's important that when trying to enforce a certain set of Pipeline Templates are used that `jte.allow_scm_jenkinsfile` is set to `False`.
    Otherwise, developers will be able to write whatever Pipeline Template they want to.

## Named Pipeline Templates

The next possibility is that the aggregated [Pipeline Configuration](../pipeline-configuration/overview.md) has configured JTE to look for a Named Pipeline Template from the [Pipeline Catalog](../pipeline-templates/pipeline-catalog.md).

If this is the case, JTE will recursively search each [Governance Tier](./governance-tier.md) in the [Configuration Hierarchy](./configuration-hierarchy.md) looking for the Named Pipeline Template.

The order of this search will be from most-granular Governance Tier to the Global Governance Tier.

If the Named Pipeline Template can't be found, the Pipeline Run will fail.

## Finding the Default Pipeline Template

Finally, if the job doesn't have a configured Pipeline Template and the Pipeline Configuration hasn't defined a Named Pipeline Template to use then JTE will search to find a Default Pipeline Template.

In this case, JTE will recursively search each Governance Tier in the Configuration Hierarchy looking for a Default Pipeline Template.

The order of this search will be from most-granular Governance Tier to the Global Governance Tier.

If a Default Pipeline Template can't be found, the Pipeline Run will fail.
