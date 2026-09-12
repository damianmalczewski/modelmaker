# example-avro

Apache Avro `.avsc` record schemas as an alternative input format, alongside this project's own Simple Schema.

> [!WARNING]
>
> **Avro support is experimental.** It is not covered by this project's compatibility promise: `AvroSchemaLoader`,
> and support for `.avsc` files in general, may change or be removed in any release, including a minor one. Use
> Simple Schema for anything that must keep working.

This does **not** produce Avro-generated classes. An `.avsc` record renders to the exact same plain immutable
POJO/DTO as a Simple Schema file, with the same optional Jackson and Jakarta Bean Validation support - it is simply
an alternative input for projects that already have their models defined as Avro schemas.

Notable differences from Simple Schema:

- A record's `namespace` / `name` map to the package and class name; the file must be named `<namespace>.<name>.avsc`.
- A field is optional only through a `["null", T]` union.
- A field `name` is used verbatim as the Java field name - no `camelCase` normalization.
- `map`, `fixed` and multi-branch unions are not supported, and a `logicalType` is ignored.

Run it with `./gradlew build` after publishing the plugin with `./gradlew publishToMavenLocal` in the root project.
