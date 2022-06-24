# Testing

JTE tests are written using [Spock](https://spockframework.org/spock/docs/2.1/index.html) and make heavy use of the [Jenkins Test Harness](https://github.com/jenkinsci/jenkins-test-harness).

## Mocking

In general, mocking is discouraged.
It is preferred that instead, the Jenkins Test Harness is used to create actual JTE Pipeline Runs that exercise the functionality to be tested.

This approach results in tests that are easier to write, decoupled from the technical implementation of the feature, and produce higher confidence that the functionality will work in a Jenkins pipeline.

Given the nuances of [CPS](https://github.com/jenkinsci/workflow-cps-plugin#technical-design) - nothing beats the real thing when validating JTE is working as expected.

!!! note "Work in Progress"

    You'll find examples of unit tests using Spock's mocking functionality.
    These tests should be refactored to use the Jenkins Test Harness as time allows.

## Test Utilities

There are several utility test classes that help perform common setup operations for tests.

| Test Class               | Description                                     |
|--------------------------|-------------------------------------------------|
| `TestFlowExecutionOwner` | simplifies mocking `FlowExecutionOwner`         |
| `TestLibraryProvider`    | a utility for creating libraries to test        |
| `TestUtil`               | a helper for creating JTE pipeline jobs to test |

## Testing Git Repositories

The [`GitSampleRepoRule`](https://github.com/jenkinsci/git-plugin/blob/master/src/test/java/jenkins/plugins/git/GitSampleRepoRule.java) from the git plugin is used to create local git repositories.

See `AdHocTemplateFlowDefinitionSpec` for examples of usage.

## Testing Pipeline Resumability

The `RestartableJenkinsRule` is used for testing that JTE pipelines can be successfully resumed after being paused or after a Jenkins graceful restart.

See `ResumabilitySpec` for examples of usage.
