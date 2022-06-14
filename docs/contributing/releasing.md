# Publishing A Release

## Prerequisites

### Release Permissions

Permissions are managed [here](https://github.com/jenkins-infra/repository-permissions-updater/blob/master/permissions/plugin-templating-engine.yml). You'll need sign-off from one of the existing maintainers to be added.

### Credentials

The [gradle plugin JTE uses](https://github.com/jenkinsci/gradle-jpi-plugin) to publish releases expects a file `.jenkins-ci.org` to be present in the user's home directory with the credentials to authenticate to the [Jenkins Artifactory](https://repo.jenkins-ci.org/) instance.

## Cutting A Release

**If you have the permission**, you can cut a new release of JTE by running `just release <versionNumber>`.

For example:

``` bash
just release 2.0.4
```

This will:

1. create a `release/2.0.4` branch
2. update the version in the `build.gradle`
3. publish a docs release to GitHub pages
4. update the bug issue template version dropdown
5. push those changes
6. create a `2.0.4` tag
7. publish the JPI

Don't forget to go to the [Release Page](https://github.com/jenkinsci/templating-engine-plugin/releases) to officially release JTE with the current change log based off the most recent tag. [Release Drafter](https://github.com/marketplace/actions/release-drafter) is used to maintain release notes for JTE.
