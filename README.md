<h1 align="center">ModelMaker</h1>

<p align="center">
  <a href="https://github.com/damianmalczewski/modelmaker/actions/workflows/ci.yml"><img src="https://github.com/damianmalczewski/modelmaker/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://codecov.io/gh/damianmalczewski/modelmaker"><img src="https://codecov.io/gh/damianmalczewski/modelmaker/graph/badge.svg?token=LHOTD2U0J2" alt="Codecov"></a>
  <a href="https://github.com/damianmalczewski/modelmaker/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green" alt="License"></a>
</p>

An **opinionated** Java/Kotlin source code generator for DTO/model classes, for use within your project. Turns small
schema files into simple, **immutable POJO** classes, with optional support for **Jackson** binding, **Jakarta Bean
Validation** and **OpenAPI** annotations.

The recommended way to use it is the `io.github.malczuuu.modelmaker` Gradle plugin, which applies an opinionated project
layout and wires code generation into the `main` source set.

📖 **[Full documentation is in the wiki.](https://github.com/damianmalczewski/modelmaker/wiki)**

## Quick Start

```kotlin
plugins {
    id("io.github.malczuuu.modelmaker") version "..."
}

modelmaker {
    features {
        jackson { enabled = true }
        validation { enabled = true }
        openApi { enabled = true }
    }
}
```

Put one schema file per type under `src/main/model`, named after its `title`:

```json
{
  "$modelmaker": "v1.0",
  "title": "com.example.dto.Person",
  "type": "object",
  "required": ["id"],
  "properties": {
    "id": { "type": "string", "description": "Person identifier.", "example": "p-42" },
    "age": { "type": "integer", "minimum": 0 }
  }
}
```

Building the project generates `com/example/dto/Person.java` into the `main` source set:

```java
package com.example.dto;

// imports ...

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

  @Schema(description = "Person identifier.", example = "p-42", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  @NotNull(message = "must not be null")
  public String getId() {
    return id;
  }

  @JsonProperty("age")
  @Min(value = 0, message = "must be greater than or equal to 0")
  public @Nullable Integer getAge() {
    return age;
  }

  // ... builder, mutate(), equals, hashCode, toString
}
```

## Documentation

- **[Getting Started](https://github.com/damianmalczewski/modelmaker/wiki/Getting-Started)** - applying the plugin,
  a first schema, where the output lands.
- **[Schema Format](https://github.com/damianmalczewski/modelmaker/wiki/Schema-Format)** - every key of the Simple
  Schema format.
- **[Generated Java Code](https://github.com/damianmalczewski/modelmaker/wiki/Generated-Java-Code)** - anatomy of a
  generated class: builder, withers, nullability.
- **[Gradle Plugin](https://github.com/damianmalczewski/modelmaker/wiki/Gradle-Plugin)** - the full `modelmaker { }`
  DSL and task reference.
- **[Jackson](https://github.com/damianmalczewski/modelmaker/wiki/Jackson)**,
  **[Validation](https://github.com/damianmalczewski/modelmaker/wiki/Validation)** and
  **[OpenAPI](https://github.com/damianmalczewski/modelmaker/wiki/OpenAPI)** - the three annotation features.
- **[Kotlin Extensions](https://github.com/damianmalczewski/modelmaker/wiki/Kotlin-Extensions)** - the generated
  `mutate { }` DSL.
- **[Library Usage](https://github.com/damianmalczewski/modelmaker/wiki/Library-Usage)** - using the core generator
  without Gradle.
- **[Troubleshooting](https://github.com/damianmalczewski/modelmaker/wiki/Troubleshooting)** - error messages and what
  causes them.

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
