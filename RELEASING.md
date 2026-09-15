# Releasing

Each module of this repository is released on its own, with its own version line:

| Module          | Artifact                                     | Published to         |
|-----------------|----------------------------------------------|----------------------|
| `modelmaker`    | `io.github.malczuuu:modelmaker`              | Maven Central        |
| `plugin-gradle` | `io.github.malczuuu.modelmaker` (plugin id)  | Gradle Plugin Portal |
| `plugin-maven`  | `io.github.malczuuu:modelmaker-maven-plugin` | Maven Central        |

A module's version lives in its own `gradle.properties`, never in the root one, and each module keeps its own
`CHANGELOG.md`. Releasing one module leaves the other two untouched.

## Steps

Run these for the module being released - `<module>` is `modelmaker`, `plugin-gradle` or `plugin-maven`.

1. Update `<module>/CHANGELOG.md` - change `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD` and add a new `[Unreleased]`
   section on top. Each module keeps its own changelog, next to the `gradle.properties` carrying its version.
2. Update `version` property in `<module>/gradle.properties` to the new version.
3. Commit the changes from (1) and (2) with a message `"Release <module> X.Y.Z"` and push to GitHub.
4. Create an annotated git tag named `<module>/vX.Y.Z` with the message `"Release <module> X.Y.Z"` and push it to
   GitHub. This triggers the release workflow, which builds the whole repository and publishes that one module. Use
   the [`./tools/tagrelease`](./tools/tagrelease) script to ensure the tag is correctly formatted - it refuses to tag
   when the version does not match the module's `gradle.properties`.
5. Update `version` property in `<module>/gradle.properties` to the next snapshot version, for example
   `1.4.1-SNAPSHOT`. Releasing a **plugin** also means pinning the examples to the version just released:
    - `plugin-gradle` - the `id("io.github.malczuuu.modelmaker") version "..."` line in each `build.gradle.kts`,
    - `plugin-maven` - the `<modelmaker.version>` property in each `pom.xml`.
6. Commit the changes from (5) with a message `"Update snapshot version"` and push to GitHub.

```bash
./tools/tagrelease modelmaker 1.2.3     # creates tag 'modelmaker/v1.2.3'
git push origin modelmaker/v1.2.3
```

The slash keeps the module and the version unambiguous whatever suffix the version carries, and lets
`git tag -l 'modelmaker/*'` list one module's releases.

Release the core before a plugin whenever the plugin depends on core changes: each plugin's published POM pins the core
version present in the tree at build time.

## Tags

The workflow triggers on `<module>/vX.Y.Z` and `<module>/vX.Y.Z-suffix` only. The tag decides everything:

| Tag                    | Module          | Gradle invocation                                                    |
|------------------------|-----------------|----------------------------------------------------------------------|
| `modelmaker/v1.2.3`    | `modelmaker`    | `-PreleaseModule=modelmaker nmcpPublishAggregationToCentralPortal`   |
| `plugin-gradle/v1.2.3` | `plugin-gradle` | `:modelmaker-gradle-plugin:publishPlugins`                           |
| `plugin-maven/v1.2.3`  | `plugin-maven`  | `-PreleaseModule=plugin-maven nmcpPublishAggregationToCentralPortal` |

Before publishing, the workflow checks the tag's version against `<module>/gradle.properties` and fails on a mismatch,
so a forgotten version bump cannot publish the wrong number.

## Maven Central

Both `modelmaker` and `plugin-maven` go to Maven Central via the Sonatype Central Portal, using
[`nmcp`](https://gradleup.com/nmcp/). Nmcp uploads an *aggregation*, configured in the root build, and `-PreleaseModule`
decides which module goes into it - so one release uploads one module:

```bash
./gradlew -Psign -PreleaseModule=modelmaker nmcpPublishAggregationToCentralPortal
```

Each published module applies `internal.nmcp-convention`, which only exposes its publications to that aggregation. The
aggregation lives in the root build because Nmcp registers its build service in a single project: applying the
aggregation plugin to more than one module fails with a missing `gratatouilleBuildService`.

The `publishingType` is `USER_MANAGED`, so the release procedure only **uploads** the artifacts. You still need to log
in to the Central Portal and publish the staged deployment by hand.

Without `-PreleaseModule` the aggregation is empty and the upload fails, which is the intended guard: a release always
names its module.

## Gradle Plugin Portal

`plugin-gradle` goes to the Gradle Plugin Portal instead, using the `com.gradle.plugin-publish` plugin:

```bash
./gradlew -Psign :modelmaker-gradle-plugin:publishPlugins
```

## Signing

Artifacts are signed only when `-Psign` is passed, which is what the release workflow does; local builds and CI skip
signing entirely, so no keys are needed to build the project.

Set the following secrets in the repository's GitHub Actions configuration:

```txt
# generated PGP key for signing artifacts
SIGNING_KEY=<PGP key>
SIGNING_PASSWORD=<PGP password>

# generated on Sonatype Central Portal, for `modelmaker` and `plugin-maven`
PUBLISHING_USERNAME=<username>
PUBLISHING_PASSWORD=<password>

# generated on Gradle Plugin Portal, for `plugin-gradle`
GRADLE_PUBLISH_KEY=<key>
GRADLE_PUBLISH_SECRET=<secret>
```

See [`release.yml`](.github/workflows/release.yml) for the exact publishing steps.

## Testing without credentials

The upload bundle can be built locally, with no Central credentials and no signing keys:

```bash
./gradlew -PreleaseModule=modelmaker nmcpZipAggregation
unzip -l build/nmcp/zip/aggregation.zip
```

It contains exactly what a release would upload - artifacts, POM and checksums - only unsigned, since signatures need
`-Psign` and a key. `nmcpPublishAggregationToMavenLocal` installs the same set into `~/.m2` instead.
