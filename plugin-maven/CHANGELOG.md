# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Added

- Add the `io.github.malczuuu:modelmaker-maven-plugin` Maven plugin, generating immutable Java DTOs from schema
  files.
- Add a `generate-java` goal bound to `generate-sources`, registering its output as a compile source root.
- Add a `<features>` configuration, exposing the core's features and their per-schema overrides.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
