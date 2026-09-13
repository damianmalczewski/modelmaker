# ModelMaker Maven Plugin - Example

The `modelmaker-maven-plugin` generating DTOs in a Maven build, with `jackson`, `validation` and `openApi` all on -
the same schemas and the same test as `example-openapi`, which does it from Gradle.

```sh
cd ../../ && ./gradlew publishToMavenLocal   # publish the plugin to ~/.m2
cd examples/example-maven && ./mvnw verify
```

`examples/buildAll` only drives the Gradle examples, so this one is run on its own, through its Maven wrapper.

The plugin binds its `generate-java` goal to `generate-sources` and registers its output as a compile source root,
so `mvn compile` picks the generated types up:

```xml
<plugin>
  <groupId>io.github.malczuuu</groupId>
  <artifactId>modelmaker-maven-plugin</artifactId>
  <version>${modelmaker.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>generate-java</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <features>
      <jackson><enabled>true</enabled></jackson>
      <validation><enabled>true</enabled></validation>
      <openApi><enabled>true</enabled></openApi>
    </features>
  </configuration>
</plugin>
```

Everything the goal accepts, besides `<features>`:

| Parameter         | Property                     | Default                                                   |
|-------------------|------------------------------|-----------------------------------------------------------|
| `schemaDirectory` | `modelmaker.schemaDirectory` | `${project.basedir}/src/main/model`                       |
| `outputDirectory` | `modelmaker.outputDirectory` | `${project.build.directory}/generated-sources/modelmaker` |
| `skip`            | `modelmaker.skip`            | `false`                                                   |

Each has a property, so they can be set from the command line without touching the pom:

```sh
./mvnw verify -Dmodelmaker.skip=true
```

The `<features>` block mirrors the Gradle DSL: `jackson`, `validation` and `openApi` take `enabled`,
`annotateFields`, `annotateGetters` (and `includeNonNull` for `jackson`), while `withers` and `preferPrimitives` take
`enabled` alone. A schema file's own `"features"` object still wins over all of it.

Unlike the Gradle plugin, the output is **not** checked in: it goes to `target/generated-sources/modelmaker`, which
is the Maven convention for generated code and the layout IDEs recognize. Point `<outputDirectory>` elsewhere if you
need to.

Maven support is an MVP: it generates Java DTOs and nothing else. Kotlin `mutate { }` extensions, and writing
generated sources into the source tree, stay Gradle-only.
