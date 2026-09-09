# Agent Instructions

## What this is

ModelMaker turns small schema files into **immutable Java DTOs** (final class, no setters, builder, `equals`/`hashCode`/
`toString`), plus optional Kotlin `mutate { }` extensions.

Problems it solves:

- Hand-written DTOs drift from the contract and are tedious to keep immutable and correct.
- Full code generators (Avro, OpenAPI Generator, jsonschema2pojo) produce heavy, mutable, or framework-coupled types.
  ModelMaker emits plain POJOs you would have written by hand.
- One schema, several concerns: JSON binding (Jackson), runtime validation (`jakarta.validation`), API docs (OpenAPI
  `@Schema`) - all toggled per project or per schema, not re-specified by hand.

The Gradle plugin is the intended entry point: drop schemas under `src/main/model`, build, and the generated types are
on the `main` source set.

## Commands

Root build default tasks: `spotlessApply build publishToMavenLocal`.

```sh
./gradlew                       # spotlessApply + build + publishToMavenLocal
./gradlew build                 # compile + tests + static analysis
./gradlew spotlessApply         # format (google-java-format / ktfmt / ktlint) + license header
./gradlew :modelmaker:test                 # core unit tests only
./gradlew :modelmaker-gradle-plugin:test   # plugin functional tests (GradleTestKit) only
```

- **Java 25 toolchain**; `main` compiles `--release 17` for consumer compatibility, modern `javac` for **ErrorProne** /
  **NullAway** (JSpecify mode, `main` only - a nullness violation fails `build`).
- `build-logic/` is an included build with the shared convention plugins.
- `examples/*` are standalone builds consuming the plugin from `mavenLocal()` - not part of the root build. Run
  `./gradlew publishToMavenLocal` first, then `examples/buildAll build`.

## Layout

| Module           | What                                                                             |
|------------------|----------------------------------------------------------------------------------|
| `:modelmaker`    | Core generator library - build-tool-agnostic, has `module-info.java`.            |
| `:plugin-gradle` | Gradle plugin (`io.github.malczuuu.modelmaker`). Wraps the core into Gradle API. |

## How generation works

`schema files -> SchemaLoader -> ModelType tree -> ModelMaker.emit(type) -> source text`

- **Loading** dispatches by extension: `.json` (this project's JSON-Schema subset) or `.avsc` (Avro). `$ref` resolution
  stays within one format's file set.
- **`ModelType`** is the resolved model - a top-level type plus nested types for inline objects.
- **`ModelMaker`** has two impls: `JavaModelMaker` (the DTO) and `KotlinExtensionMaker` (the `<Name>Extensions.kt`).
  Java output is assembled from small focused `*SnippetRenderer` pieces (fields, constructor, getters, builder, …).

### Feature flags

`jackson`, `validation`, `openApi`, `withers`, `preferPrimitives`. The three annotation features also choose **where**
their annotations sit - the fields, the getters, or both (default: getters).

Resolved highest-priority-first: a schema file's top-level `"features"` object -> a per-task `features { }` -> the
project-wide `modelmaker { features { } }` default.

## Plugin

`ModelMakerPlugin` registers the `modelmaker { }` extension and two tasks: `generateModelJava` and `generateModelKotlin`
(depends on it). On `JavaPlugin` it puts both output dirs on the `main` source set and adds `org.jspecify:jspecify`
(`compileOnly`) unless already declared.

- Output location: `modelmaker.src.enabled = true` (default) writes checked-in `src/main/model/{java,kotlin}`; `false`
  writes under `build/generated/`.
- Tasks are `@CacheableTask` but **not incremental** - the output dir is wiped and rebuilt each run.
- `generateModelKotlin` doubles as a cleaner: with Kotlin off it deletes its output dir rather than leaving stale `.kt`.

## Conventions

- Spotless enforces the license header (`gradle/license-header.txt`) on every `*.java`/`*.kt`/`*.kts` under `src/`. No
  wildcard imports in Java.
- `:plugin-gradle` has `explicitApi()` - every public declaration needs an explicit modifier and return type.
- Tests run `per_class` lifecycle - use proper `@BeforeEach` / `@AfterEach` (or `@BeforeAll` on an instance method).
