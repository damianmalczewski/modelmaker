# ModelMaker Gradle Plugin

Gradle plugin that applies ModelMaker conventions and generates immutable Java model classes (and, opt-in, Kotlin
extensions) from schema files, wired into the `main` source set.

```kotlin
plugins {
    id("io.github.malczuuu.modelmaker") version "..."
}
```

## Schema files

Place schema files under `src/main/model`, one type per file:

- **Simple Schema** (`.json`) - this project's own lightweight JSON-Schema-alike format.
- **Apache Avro** (`.avsc`) - accepted alongside Simple . It does **not** produce Avro-generated classes: an `.avsc`
  record renders to the exact same kind of plain immutable POJO/DTO as a Simple Schema file, with the same optional
  Jackson and/or Jakarta Validation support.

## Tasks

- `generateModelJava` - one `.java` class per schema file.
- `generateModelKotlin` - depends on `generateModelJava`; one `mutate { }` Kotlin extension per type, only when
  `kotlin.enabled` is on **and** the Kotlin JVM plugin is applied.

## Configuration

```kotlin
modelmaker {
    src {
        enabled = true  // default: write into src/main/model/{java,kotlin} (checked in);
    }                   // false writes into build/generated/sources/modelmaker instead
    kotlin {
        enabled = false  // default off
    }
    features {
        jackson          = false  // @JsonCreator / @JsonProperty / ...
        validation       = false  // jakarta.validation annotations, @Valid cascades
        withers          = false  // withXyz(value) per property
        preferPrimitives = false  // int/double/boolean instead of boxed types
    }
}
```

Any `features` flag can be overridden per schema file via a top-level `"features"` object in that file, taking
precedence over the project default for that type only.

The plugin adds `org.jspecify:jspecify` on `compileOnly` unless the build already declares it, as generated classes are
`@NullMarked`.
