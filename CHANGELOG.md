# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Added

- Add the `io.github.malczuuu.modelmaker` Gradle plugin, generating immutable Java DTOs from schema files.
- Add the `generateModelJava` and `generateModelKotlin` tasks.
- Add the Simple Schema format (`$modelmaker: "v1.0"`).
- Add Kotlin `mutate { }` extensions.
- Add the `jackson`, `validation` and `openApi` features.
- Add the `withers` and `preferPrimitives` features.
- Add the `"sensitive"` property keyword.
- Add per-schema feature overrides.
- Add `modelmaker { schemas { } }` and `modelmaker { src { } }` configuration.
- Add `io.github.malczuuu:modelmaker`, the build-tool-agnostic core, for use without Gradle.
- Add `SchemaException`, reporting every schema problem of a run at once.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
