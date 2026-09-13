# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Added

- Add the `io.github.malczuuu.modelmaker` Gradle plugin, generating immutable Java DTOs from schema files.
- Add the `generateModelJava`, `generateModelKotlin` and `generateModel` tasks.
- Add `modelmaker { features { } }`, exposing the core's features and their per-schema overrides.
- Add `modelmaker { schemas { } }` and `modelmaker { src { } }` configuration.
- Add `modelmaker { kotlin { } }`, emitting the core's `mutate { }` extensions.
- Add `org.jspecify:jspecify` on `compileOnly` unless the build already declares it.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
