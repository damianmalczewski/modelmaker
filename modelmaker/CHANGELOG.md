# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Added

- Add `io.github.malczuuu:modelmaker`, the schema loaders and source emitters shared by the build-tool plugins.
- Add the Simple Schema format (`$modelmaker: "v1.0"`).
- Add the `jackson`, `validation` and `openApi` features.
- Add the `withers` and `preferPrimitives` features.
- Add the `"sensitive"` property keyword.
- Add per-schema feature overrides.
- Add Kotlin `mutate { }` extensions.
- Add `SchemaException`, reporting every schema problem of a run at once.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
