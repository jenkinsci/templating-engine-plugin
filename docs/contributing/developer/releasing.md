# Publishing A Release

**If you have the permission**, you can cut a new release of JTE by running `just release <versionNumber>`.

For example:

```bash
just release 2.0.4
```

This will:

1. create a `release/2.0.4` branch
2. update the version in the `build.gradle`
3. update the version in the `docs/antora.yml`
4. push those changes
5. create a `2.0.4` tag
6. publish the JPI

Don't forget to go to the [Release Page](https://github.com/jenkinsci/templating-engine-plugin/releases) to officially release JTE with the current change log based off the most recent tag.

!!! note "Release Permissions"
    Permissions are managed [here](https://github.com/jenkins-infra/repository-permissions-updater/blob/master/permissions/plugin-templating-engine.yml). You'll need sign-off from one of the existing maintainers to be added.
