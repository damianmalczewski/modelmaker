# Agent Instructions

Project's purpose is generation of immutable Java DTO models with optional Kotlin extensions based on schema files.

## Commands

Root build uses default tasks `spotlessApply build publishToMavenLocal`.

```sh
./gradlew                       # spotlessApply + build + publishToMavenLocal
./gradlew build                 # compile + unit/functional tests + static analysis (NullAway)
./gradlew spotlessApply         # format: google-java-format (*.java), ktfmt (*.kt), ktlint (*.gradle.kts); enforces license header
./gradlew spotlessCheck         # verify formatting without changing files
./gradlew :modelmaker:test      # core library unit tests only
./gradlew :modelmaker-gradle-plugin:test   # plugin functional tests (GradleTestKit) only
./gradlew jacocoTestReport      # coverage (XML + HTML); `check` also finalizes with it
```

- **Java 25 toolchain** is provisioned by Gradle; `main` sources compile with `--release 17` for consumer binary
  compatibility while still using a modern `javac` for **ErrorProne** and **NullAway**.
- Static analysis (**NullAway**, **JSpecify** mode) runs on `main` sources only; test compilation is unconstrained. A
  nullness violation fails `build`.
- `build-logic/` is an included build; providing common convention plugins.

### Examples

`examples/*` are standalone Gradle builds that consume the published plugin from `mavenLocal()`, not part of the root
build.

```sh
./gradlew publishToMavenLocal   # required first
examples/buildAll build         # runs `./gradlew build` in every example
examples/buildAll clean         # (or any task list) forwarded to each example
```

## Architecture

Two published modules plus an included build:

| Module           | Coordinates                                      | Notes                                                                                               |
|------------------|--------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `:modelmaker`    | `io.github.malczuuu:modelmaker`                  | Core generator library. `java-library`, has `module-info.java`, build-tool-agnostic.                |
| `:plugin-gradle` | Gradle plugin ID `io.github.malczuuu.modelmaker` | Kotlin `kotlin-dsl` with `explicitApi()`; published to the Plugin Portal. Depends on `:modelmaker`. |

### Generation pipeline (`:modelmaker`)

Schema file (s) -> `SchemaLoader` -> `ModelType` tree -> `ModelMaker.emit(type)` -> source text.

- **Loading**: `SchemaLoaders.createDelegatingSchemaLoader()` dispatches by file extension — `.json` to
  `SimpleSchemaLoader` (this project's JSON-Schema subset) or `.avsc` to `AvroSchemaLoader`. Both are usable standalone.
  Named-type / `$ref` resolution happens **within one format's file set only** — there is no cross-format reference
  resolution.
- **`ModelType`** is a tree: a top-level type plus inline `object` properties as nested types.
- **`ModelMaker`** is a `sealed interface` with `emit(ModelType) -> String`. Two impls: `JavaModelMaker` (producing
  final immutable POJO, builder, `equals`/`hashCode`/`toString`, optional Jackson/validation annotations) and
  `KotlinExtensionMaker` (producing `<Name>Extensions.kt` with a extension methods).
- **Rendering** of code snippets is split across `*SnippetRenderer` classes for convenience (`SnippetRenderer` sealed
  interface; `AbstractSnippetRenderer` gives the common `(indent, properties, options, types)` setup). Each renders one
  focused chunk (fields, constructor, getters, builder, withers, `mutate`, …) plus the imports it needs, as a
  `RenderResult`. `JavaModelMaker` / `KotlinExtensionMaker` compose them and assemble the `package` line, import block,
  do-not-edit banner and class body.
- **Feature flags**: `jackson`, `validation`, `withers`, `preferPrimitives`. Precedence, highest first: per-schema
  `"features"` object -> per-task `features { }` (or `kotlin { }`) -> `modelmaker { features { } }` project default.

### Plugin wiring (`:plugin-gradle`)

`ModelMakerPlugin.apply`:

1. Registers the `modelmaker { }` extension (`ModelMakerExtension` + `src`/`kotlin`/`features` specs) with conventions.
2. Registers `generateModelJava` (`JavaModelGenerate`) scanning `src/main/model/**/*.{json,avsc}`, and
   `generateModelKotlin` (`KotlinModelGenerate`, `dependsOn` the Java task).
3. On `JavaPlugin`: adds both output dirs as `srcDir`s of the `main` source set, so `javac` / the Kotlin plugin pick the
   generated code up; also adds `org.jspecify:jspecify` on `compileOnly` unless the build already declares it (generated
   code is `@NullMarked`).

Output location depends on `modelmaker.src.enabled`: `true` (default) writes checked-in `src/main/model/{java,kotlin}`;
`false` writes `build/generated/sources/modelmaker/...`.

Tasks are `@CacheableTask` with `sourceDirectory` tracked `RELATIVE`, but **not incremental** — the output dir is wiped
and rebuilt on every run. `generateModelKotlin` also acts as a cleaner: when `kotlin.enabled` is off or the Kotlin JVM
plugin is absent it deletes its output dir entirely (removes stale `.kt` rather than leaving it), rather than
generating.

## Conventions

- License header (`gradle/license-header.txt`) is enforced by Spotless on every `*.java`/`*.kt`/`*.kts` under `src/`
  (examples excluded — each example has its own Spotless config). Wildcard imports are forbidden in Java.
- `:plugin-gradle` has `explicitApi()` on: every public declaration needs an explicit visibility modifier and return
  type.
- Tests use global `junit.jupiter.testinstance.lifecycle.default=per_class` convention, so proper `@Before...` and
  `@After...` setup is required.
