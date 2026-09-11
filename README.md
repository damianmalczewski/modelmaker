# ModelMaker

An **opinionated** Java/Kotlin source code generator for DTO/model classes, for use within your project. Turns small
schema files into simple, **immutable POJO** classes, with optional support for **Jackson** deserialization and
**Jakarta Bean Validation**.

The recommended way to use it is the `io.github.malczuuu.modelmaker` Gradle plugin, which applies an opinionated project
layout and wires code generation into the `main` source set.

## Table of Contents

- [Quick Start](#quick-start)
- [Generated Java Classes](#generated-java-classes)
- [Generated Kotlin Code](#generated-kotlin-code)
- [Gradle Plugin](#gradle-plugin)
  - [Schema files](#schema-files)
  - [Tasks](#tasks)
  - [Configuration](#configuration)
- [Simple Schema Format](#simple-schema-format)
- [Apache Avro Format](#apache-avro-format)
- [Library Usage](#library-usage)
- [Examples](#examples)
- [Building](#building)
- [License](#license)

## Quick Start

```kotlin
plugins {
    id("io.github.malczuuu.modelmaker") version "..."
}
```

Place schema files under `src/main/model`, one type per file (see [Simple Schema Format](#simple-schema-format) and
[Apache Avro Format](#apache-avro-format)). Building the project generates one immutable Java class per schema, wired
into the `main` source set.

## Generated Java Classes

The `JavaModelMaker` emits a `final`, immutable Java POJO/DTO class with a `private` (or `package-private`)
constructor, instantiated via its builder, plus `equals`, `hashCode`, and `toString`. A `List<T>` field is copied on
the way in and returned as an unmodifiable view; a `bytes` (`byte[]`) field is cloned by the builder setter, by every
getter, and by a `withXyz` replacement, so a caller's array is never aliased by the DTO; `equals`/`hashCode` compare it
by content and `toString` prints it as its base64 string.

Features configurable via `ModelOptions` and `"features"` section in schema files:

- `jackson` - adds `@JsonCreator`, `@JsonProperty` and friends for JSON deserialization, also makes the constructor
  `package-private` to allow deserialization.
- `validation` - adds `jakarta.validation` constraint annotations, according to rules from schema file.
- `openApi` - adds OpenAPI `@Schema` annotations (`io.swagger.v3.oas.annotations.media.Schema`) carrying each schema's
  `description` / `example` and, for a required property, `requiredMode`. Facets such as `minimum` / `pattern` are left
  to the `validation` feature, which the OpenAPI tooling already reads. Needs `io.swagger.core.v3:swagger-annotations-jakarta`
  on the consumer's compile classpath.
- `withers` - adds `withXyz(value)` methods, which are per property, single-field copy methods.
- `preferPrimitives` - force usage of type primitives (`int`/`double`/`boolean`) instead of their boxed types if
  possible (for example, integer with default value will produce `int`).

`jackson`, `validation` and `openApi` are each a block, not a bare boolean: `{ enabled; annotateFields; annotateGetters }`
- `enabled` turns the feature on, `annotateFields` / `annotateGetters` choose where its annotations are placed
(defaults: `enabled = false`, `annotateFields = false`, `annotateGetters = true`, so an enabled feature annotates the
getters). Setting both `annotateFields` and `annotateGetters` emits on both - for `validation` that means Hibernate
Validator visits each constraint twice. `withers` and `preferPrimitives` stay plain `{ enabled }` toggles.

Generated classes are annotated with **JSpecify** annotations - `@NullMarked` on a class and `@Nullable` on every
optional field/parameter.

> [!NOTE]
>
> Generated models support simple types only - the ones most likely to serialize and deserialize cleanly between a
> Java object and JSON. An `enum`, for example, becomes a plain `String` field with a generated `@Pattern` validation
> over the allowed values and a `"must be one of ..."` message.
>
> This can be worked around, to some extent, by referencing (`"$ref"`) an existing type instead.

## Generated Kotlin Code

The `KotlinExtensionMaker` emits a `mutate { }` Kotlin extension function per type, wrapping the Java `Builder` in a
Kotlin-idiomatic, DSL-alike block.

```kotlin
public inline fun Person.mutate(block: Person.BuilderMutator.() -> Unit): Person =
    this.mutate().also { it.block() }.build()
```

## Gradle Plugin

Gradle plugin that applies ModelMaker conventions and generates immutable Java model classes (and, opt-in, Kotlin
extensions) from schema files, wired into the `main` source set.

```kotlin
plugins {
    id("io.github.malczuuu.modelmaker") version "..."
}
```

### Schema files

Place schema files under `src/main/model`, one type per file:

- **Simple Schema** (`.json`) - this project's own lightweight JSON-Schema-alike format.
- **Apache Avro** (`.avsc`) - accepted alongside Simple Schema. It does **not** produce Avro-generated classes: an
  `.avsc` record renders to the exact same kind of plain immutable POJO/DTO as a Simple Schema file, with the same
  optional Jackson and/or Jakarta Validation support.

### Tasks

- `generateModelJava` - one `.java` class per schema file.
- `generateModelKotlin` - depends on `generateModelJava`; one `mutate { }` Kotlin extension per type, only when
  `kotlin.enabled` is on **and** the Kotlin JVM plugin is applied.

### Configuration

```kotlin
modelmaker {
    src {
        enabled = true  // default: write into src/main/model/{java,kotlin} (checked in);
    }                   // false writes into build/generated/sources/modelmaker instead
    kotlin {
        enabled = false  // default off
    }
    features {
        jackson {          // @JsonCreator / @JsonProperty / ...
            enabled         = false
            annotateFields  = false
            annotateGetters = true
        }
        validation { enabled = false }  // jakarta.validation annotations, @Valid cascades
        openApi    { enabled = false }  // OpenAPI @Schema (description / example / requiredMode)
        withers          { enabled = false }  // withXyz(value) per property
        preferPrimitives { enabled = false }  // int/double/boolean instead of boxed types
    }
}
```

Any `features` flag can be overridden per schema file via a top-level `"features"` object in that file, taking
precedence over the project default for that type only. Each entry is an object matching the DSL shape - `{ "enabled":
..., "annotateFields": ..., "annotateGetters": ... }` for `jackson` / `validation` / `openApi`, `{ "enabled": ... }`
for `withers` / `preferPrimitives`; any key left out keeps the project default:

```json
"features": {
  "jackson":    { "enabled": true },
  "validation": { "enabled": true, "annotateFields": true, "annotateGetters": false }
}
```

The plugin adds `org.jspecify:jspecify` on `compileOnly` unless the build already declares it, as generated classes are
`@NullMarked`.

## Simple Schema Format

The built-in format for defining models is a Simple Schema, a slightly altered JSON Schema. A file describes one type as
a JSON object:

| Key           | Required | Meaning                                                                                            |
|---------------|----------|----------------------------------------------------------------------------------------------------|
| `$modelmaker` | yes      | schema version; only `"v1.0"` is currently supported                                               |
| `title`       | yes      | fully-qualified class name, e.g. `"com.example.dto.Person"`; the file must be named `<title>.json` |
| `type`        | yes      | must be `"object"`                                                                                 |
| `properties`  | yes      | object of property name -> property schema                                                         |
| `required`    | no       | array of property names that must always be set                                                    |
| `description` | no       | emitted as `@Schema(description = ...)` on the generated class when `openApi` is on                |
| `features`    | no       | per-file override of `modelmaker { features { } }`, see below                                      |

Each entry under `properties` is itself a small schema, either a `"type"` or a `"$ref"`:

| `type`    | Renders as          | Notes                                                                                                                          |
|-----------|---------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `string`  | `String`            |                                                                                                                                |
| `bytes`   | `byte[]`            | stored and returned as a defensive copy; serialized by Jackson as a base64 string; no `"default"` support                      |
| `integer` | `Integer`/`int`     | `"format"`: `"int"` (default) or `"long"` -> `Long`/`long`                                                                     |
| `number`  | `Double`/`double`   | `"format"`: `"float"` -> `Float`/`float`                                                                                       |
| `boolean` | `Boolean`/`boolean` |                                                                                                                                |
| `object`  | a nested class      | inline; `"title"` names it (plain class name, no package), else the capitalized property name                                  |
| `array`   | `List<T>`           | needs `"items"`, a nested property schema; arrays of arrays are not supported; `"items"` may carry its own validation keywords |
| `$ref`    | another type        | a schema `title` in the same directory, or a `java.*`/`javax.*` type such as `"java.time.Instant"` (imported, not generated)   |

A property not listed in `required` is optional (`@Nullable` in the generated field/parameter).
`"default"` gives it a value applied by the builder when left unset.

**Property key naming.** A property key must be one of these forms, **optionally prefixed with one or more
underscores** (e.g. `__metadata`):

| Form             | Example      | Regex (after the underscore prefix) |
|------------------|--------------|-------------------------------------|
| `camelCase`      | `firstName`  | `[a-z][a-zA-Z0-9]*`                 |
| `snake_case`     | `first_name` | `[a-z][a-z0-9]*(_[a-z0-9]+)*`       |
| `UpperCamelCase` | `FirstName`  | `[A-Z][a-zA-Z0-9]*`                 |

The key is normalized to a `camelCase` field name: `snake_case` segments are joined and capitalized, an
`UpperCamelCase` key is lower-cased on its first letter, and any leading-underscore prefix is **kept** as-is. So
`first_name`, `FirstName` and `firstName` all become the field `firstName`, while `_first_name` becomes `_firstName`
(distinct from `firstName`). The original key is preserved for `@JsonProperty` / `@JsonPropertyOrder`.

The getter / builder-method / wither names are derived from the field name by capitalizing its first letter, so an
underscore-prefixed field `_firstName` yields `get_firstName()` / `_firstName(...)` / `with_firstName(...)` - valid
Java, but not a JavaBeans-style accessor.

Rejected at load time (not left to fail `javac`):

- a key that matches none of the forms above - e.g. a trailing or doubled underscore (`name_`, `first__name`),
  `kebab-case`, or a leading digit;
- two keys in the same object that normalize to the same field name (`first_name` + `firstName`, or
  `_first_name` + `_firstName`).

Validation keywords, translated to `jakarta.validation` annotations when the `validation` feature is on (see
[Generated Java Classes](#generated-java-classes)); ignored otherwise:

| Keyword                   | Applies to | Annotation                                                                                           |
|---------------------------|------------|------------------------------------------------------------------------------------------------------|
| `pattern`                 | `string`   | `@Pattern`                                                                                           |
| `enum`                    | `string`   | `@Pattern` alternation over the listed values, see [Generated Java Classes](#generated-java-classes) |
| `format: "email"`         | `string`   | `@Email`                                                                                             |
| `format` (other)          | `string`   | `@Pattern` for the matching shape - `decimal`, `date`, `time`, `date-time`, `uuid`                   |
| `minLength` / `maxLength` | `string`   | `@Size`                                                                                              |
| `minimum` / `maximum`     | `integer`  | `@Min` / `@Max`                                                                                      |
| `minimum` / `maximum`     | `number`   | `@DecimalMin` / `@DecimalMax`                                                                        |
| `minItems` / `maxItems`   | `array`    | `@Size` on the `List`                                                                                |

**List elements.** The `string` / `integer` / `number` keywords above (`pattern`, `enum`, `format`, `minLength` /
`maxLength`, `minimum` / `maximum`) also apply when written **inside an array's `"items"`**, and are emitted as
container-element annotations, e.g. `List<@Size(min = 1, max = 255) String> tags`. `minItems` / `maxItems` stay on the
array node and size the `List` itself. Element annotations - and the `@Valid` cascade into a `List` of generated types -
render at the same site as the property's own constraints (the getter by default), on the field / getter type only; the
constructor and builder keep the plain `List<T>`.

**Placement.** `validation` and `openApi` annotations sit on the getters by default (`annotateGetters = true`), matching
where `jackson` puts `@JsonProperty`. Set `annotateFields = true` (and, if you want field placement only,
`annotateGetters = false`) to move or duplicate them onto the `private final` fields.

OpenAPI keywords, emitted as `@Schema` arguments when the `openApi` feature is on; ignored otherwise:

| Keyword       | Applies to        | Emitted as                                                         |
|---------------|-------------------|--------------------------------------------------------------------|
| `description` | type or property  | `@Schema(description = ...)` on the class / on the getter or field |
| `example`     | property (scalar) | `@Schema(example = "...")`, rendered as a string                   |
| `required`    | property          | `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)`             |

A property's `@Schema` is only emitted when it carries a `description` or `example`. Facets such as `minimum` /
`pattern` are deliberately left off `@Schema` - the `validation` feature emits them as `jakarta.validation`
annotations, which OpenAPI generators (springdoc, swagger-core) already read. For Avro input the record / field
`"doc"` is used as the `description`.

The `features` section (`jackson`, `validation`, `openApi`, `withers`, `preferPrimitives`) allows overriding the
global `ModelOptions` on a per-file basis.

<details>
<summary><b>Example (expand)...</b></summary>

```json
{
  "$modelmaker": "v1.0",
  "title": "com.example.dto.Person",
  "type": "object",
  "required": ["id"],
  "properties": {
    "id": { "type": "string" },
    "age": { "type": "integer" }
  },
  "features": {
    "jackson": { "enabled": true },
    "validation": { "enabled": true }
  }
}
```

Renders to the following `Person.java` file.

```java
@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "age"})
public final class Person {

  private final String id;

  private final @Nullable Integer age;

  @JsonCreator
  Person(
      @JsonProperty("id") String id,
      @JsonProperty("age") @Nullable Integer age) {
    this.id = id;
    this.age = age;
  }

  @JsonProperty("id")
  @NotNull(message = "must not be null")
  public String getId() {
    return id;
  }

  @JsonProperty("age")
  public @Nullable Integer getAge() {
    return age;
  }

  // builder utilities and equals, hashCode, toString
}
```

</details>

## Apache Avro Format

The `.avsc` format is supported too - Avro is a well-established, widely-used schema format in its own right, so it's
accepted as an alternate input alongside Simple Schema.

> [!IMPORTANT]
>
> This does **not** produce Avro-generated classes. An `.avsc` file renders to the exact same plain immutable
> POJO/DTO as Simple Schema, with the same support for Jackson and Jakarta Bean Validation. It's simply an alternative
> input for projects that already have their models defined as Avro schemas.

1. A record's `namespace`/`name` map to package/class name (file must be named `<namespace>.<name>.avsc`);
2. Types `int`/`long`, `float`/`double`, `string`, `bytes`, `boolean` map to their Simple Schema equivalents
   (`bytes` -> `byte[]`, base64 on the wire);
3. A nested `record` becomes a nested class;
4. An `array` needs `items`;
5. A field is optional only through a `["null", T]` union, with `"default"` applied the same way;
6. Types `map`, `fixed`, and multi-branch unions are not supported;
7. A `logicalType` is ignored (the field maps to its base Avro type).
8. A field `name` is used verbatim as the Java field name - no `camelCase` normalization, unlike Simple Schema - so it
   must already be a valid Java identifier; a record that declares the same field `name` twice is rejected at load time.

An Avro schema passed to ModelMaker can carry the same additional `"features"` section as Simple Schema: a per-file
override of `ModelOptions`.

<details>
<summary><b>Example (expand)...</b></summary>

The same example as above can be generated using the following Avro, with `jackson` and `validation` on:

```json
{
  "type": "record",
  "namespace": "com.example.dto",
  "name": "Person",
  "fields": [
    { "name": "id", "type": "string" },
    { "name": "age", "type": ["null", "int"], "default": null }
  ],
  "features": {
    "jackson": { "enabled": true },
    "validation": { "enabled": true }
  }
}
```

It renders to the exact same `Person.java` shown above.

</details>

## Library Usage

If you cannot use the Gradle plugin, scan schema directories, create a `JavaModelMaker`, and use it to generate Java
code for each schema.

```java
File schemasDir = ...;
File outputDir = ...;

List<Path> schemas = Files.walk(schemasDir.toPath())
    .filter(path -> Files.isRegularFile(path))
    .filter(path -> {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".json") || fileName.endsWith(".avsc");
    })
    .collect(Collectors.toList());

if (schemas.isEmpty()) {
    return;
}

JavaModelMaker javaModelMaker = new JavaModelMaker(ModelOptions.builder().build());

SchemaLoaders.createDelegatingSchemaLoader().load(schemas).forEach(
    type -> {
        String javaSrc = javaModelMaker.emit(type);
        write(outputDir, type.getPackageName(), type.getName() + ".java", javaSrc);
    });
```

## Examples

Standalone Gradle builds under `examples/`, each applying the plugin from `mavenLocal()`. Run all of them with
`examples/buildAll <tasks>` (e.g. `examples/buildAll build`) after `./gradlew publishToMavenLocal`.

| Example              | Shows                                                                |
|----------------------|----------------------------------------------------------------------|
| `example-jackson`    | `jackson` feature (`tools.jackson`, Jackson 3) plus Kotlin `mutate`. |
| `example-validation` | `validation` feature and `jakarta.validation` constraints.           |
| `example-openapi`    | `jackson` + `validation` + `openApi` on one type; OpenAPI `@Schema`. |
| `example-mutator`    | Kotlin `mutate { }` extensions.                                      |
| `example-avro`       | `.avsc` schema input.                                                |

## Building

```sh
./gradlew                      # calls configured Gradle default tasks (spotlessApply, build, publishToMavenLocal)
./gradlew spotlessApply        # format code: ktfmt for *.kt (Kotlin sources), ktlint for *.kts (Gradle buildscript)
./gradlew build                # compiles the plugin and runs its unit tests
./gradlew publishToMavenLocal  # publishes artifacts to local ~/.m2 to use local snapshots
```

## License

This project is licensed under the Apache License, Version 2.0.

This project is not affiliated with, sponsored by, or endorsed by Gradle. All product names, logos, and brands are
property of their respective owners.
