# ModelMaker

An **opinionated** Java/Kotlin source code generator for DTO/model classes, for use within your project. Turns small
schema files into simple, **immutable POJO** classes, with optional support for **Jackson** deserialization and
**Jakarta Bean Validation**.

## Table of Contents

- [Generated Java Classes](#generated-java-classes)
- [Generated Kotlin Code](#generated-kotlin-code)
- [Usage](#usage)
- [Simple Schema Format](#simple-schema-format)
- [Apache Avro Format](#apache-avro-format)
- [Building](#building)

## Generated Java Classes

The `JavaModelMaker` emits a `final`, immutable Java POJO/DTO class with a `private` (or `package-private`)
constructor, instantiated via its builder, plus `equals`, `hashCode`, and `toString`.

Features configurable via `ModelOptions` and `"features"` section in schema files:

- `jackson` - adds `@JsonCreator`, `@JsonProperty` and friends for JSON deserialization, also makes the constructor
  `package-private` to allow deserialization.
- `validation` - adds `jakarta.validation` constraint annotations, according to rules from schema file.
- `withers` - adds `withXyz(value)` methods, which are per property, single-field copy methods.
- `preferPrimitives` - force usage of type primitives (`int`/`double`/`boolean`) instead of their boxed types if
  possible (for example, integer with default value will produce `int`).

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

## Usage

Scan schema directories, create `JavaModelMaker` and use it to generate Java code for each schema.

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

The recommended way however is to use `io.github.malczuuu.modelmaker` plugin for Gradle and benefit from an opinionated
project layout.

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
| `description` | no       | copied onto the generated class as its Javadoc                                                     |
| `features`    | no       | per-file override of `modelmaker { features { } }`, see below                                      |

Each entry under `properties` is itself a small schema, either a `"type"` or a `"$ref"`:

| `type`    | Renders as          | Notes                                                                                                                        |
|-----------|---------------------|------------------------------------------------------------------------------------------------------------------------------|
| `string`  | `String`            |                                                                                                                              |
| `integer` | `Integer`/`int`     | `"format"`: `"int"` (default) or `"long"` -> `Long`/`long`                                                                   |
| `number`  | `Double`/`double`   | `"format"`: `"float"` -> `Float`/`float`                                                                                     |
| `boolean` | `Boolean`/`boolean` |                                                                                                                              |
| `object`  | a nested class      | inline; `"title"` names it (plain class name, no package), else the capitalized property name                                |
| `array`   | `List<T>`           | needs `"items"`, a nested property schema; arrays of arrays are not supported                                                |
| `$ref`    | another type        | a schema `title` in the same directory, or a `java.*`/`javax.*` type such as `"java.time.Instant"` (imported, not generated) |

A property not listed in `required` is optional (`@Nullable` in the generated field/parameter).
`"default"` gives it a value applied by the builder when left unset.

Validation keywords, translated to `jakarta.validation` annotations when the `validation` feature is on (see
[Generated Java Classes](#generated-java-classes)); ignored otherwise:

| Keyword                                              | Applies to | Annotation                                                                                           |
|------------------------------------------------------|------------|------------------------------------------------------------------------------------------------------|
| `pattern`                                            | `string`   | `@Pattern`                                                                                           |
| `enum`                                               | `string`   | `@Pattern` alternation over the listed values, see [Generated Java Classes](#generated-java-classes) |
| `format: "email"`                                    | `string`   | `@Email`                                                                                             |
| `format: "decimal"/"date"/"time"/"date-time"/"uuid"` | `string`   | `@Pattern` for the matching shape                                                                    |
| `minLength` / `maxLength`                            | `string`   | `@Size`                                                                                              |
| `minimum` / `maximum`                                | `integer`  | `@Min` / `@Max`                                                                                      |
| `minimum` / `maximum`                                | `number`   | `@DecimalMin` / `@DecimalMax`                                                                        |
| `minItems` / `maxItems`                              | `array`    | `@Size`                                                                                              |

The `features` section (`jackson`, `validation`, `withers`, `preferPrimitives`) allows overriding the global
`ModelOptions` on a per-file basis.

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
    "jackson": true,
    "validation": true
  }
}
```

Renders to the following `Person.java` file.

```java
@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "age"})
public final class Person {

  @NotNull(message = "must not be null")
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
> This module does **not** produce Avro-generated classes. An `.avsc` file renders to the exact same plain immutable
> POJO/DTO as Simple Schema, with the same support for Jackson and Jakarta Bean Validation. It's simply an alternative
> input for projects that already have their models defined as Avro schemas.

1. A record's `namespace`/`name` map to package/class name (file must be named `<namespace>.<name>.avsc`);
2. Types `int`/`long`, `float`/`double`, `string`/`bytes`, `boolean` map to their Simple Schema equivalents;
3. A nested `record` becomes a nested class;
4. An `array` needs `items`;
5. A field is optional only through a `["null", T]` union, with `"default"` applied the same way;
6. Types `map`, `fixed`, and multi-branch unions are not supported;
7. A `logicalType` is ignored (the field maps to its base Avro type).

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
    "jackson": true,
    "validation": true
  }
}
```

It renders to the exact same `Person.java` shown above.

```java
@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "age"})
public final class Person {

  @NotNull(message = "must not be null")
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

## Building

```sh
./gradlew                  # calls configured Gradle default tasks (spotlessApply and build)
./gradlew spotlessApply    # format code: ktfmt for *.kt (Kotlin sources), ktlint for *.kts (Gradle buildscript)
./gradlew build            # compiles the plugin and runs its unit tests
```

## License

This project is licensed under the Apache License, Version 2.0.
