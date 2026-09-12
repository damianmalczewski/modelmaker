/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.malczuuu.modelmaker

import io.github.malczuuu.modelmaker.fixtures.TestProject
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelMakerPluginFunctionalTest {

  @TempDir lateinit var projectDir: File

  private lateinit var project: TestProject

  @BeforeEach
  fun beforeEach() {
    project = TestProject(projectDir)
  }

  @Test
  fun `generates and compiles plain java models by default - no jackson or validation`() {
    project.writeStandardBuild()
    project.writeSchemas()

    val result = project.runner("compileJava").build()

    assertThat(result.task(":generateModelJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    val java = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(java).contains("public final class Customer {")
    assertThat(java).contains("@NullMarked")
    assertThat(java).contains("private Customer(")
    assertThat(java).contains("private final Integer age;") // boxed scalars by default
    assertThat(java).doesNotContain("jackson")
    assertThat(java).doesNotContain("jakarta.validation")
    assertThat(java).doesNotContain("@JsonProperty")
    assertThat(java).doesNotContain("@NotNull")

    val classpath =
        project.runner("dependencies", "--configuration", "compileClasspath").build().output
    assertThat(classpath).doesNotContain("jackson")
    assertThat(classpath).doesNotContain("jakarta.validation")
    assertThat(classpath).contains("org.jspecify:jspecify")
  }

  @Test
  fun `jacksonAnnotations opt-in emits jackson annotations and compiles against declared jackson`() {
    project.writeStandardBuild(
        modelMakerBlock = "modelmaker { features { jackson { enabled = true } } }",
        dependencies = "implementation(\"com.fasterxml.jackson.core:jackson-annotations:2.18.2\")",
    )
    project.writeSchemas()

    val result = project.runner("compileJava").build()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    val java = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(java).contains("@JsonCreator")
    assertThat(java).contains("import com.fasterxml.jackson.annotation.JsonCreator;")
  }

  @Test
  fun `a schema's own features override wins over the project default`() {
    project.writeStandardBuild(
        dependencies = "implementation(\"com.fasterxml.jackson.core:jackson-annotations:2.18.2\")"
    )
    project.writeSchemas()
    project.write(
        "src/main/model/com.example.dto.Receipt.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Receipt", "type": "object",
        "features": { "jackson": { "enabled": true } },
        "required": ["id"], "properties": { "id": { "type": "string" } } }
      """,
    )

    val result = project.runner("compileJava").build()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    // Overridden on: this schema gets Jackson annotations even though the project default is off.
    val receipt = project.generatedJava("com/example/dto/Receipt.java").readText()
    assertThat(receipt).contains("@JsonCreator")

    // Not overridden: the project default (off) still applies to every other schema.
    val customer = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(customer).doesNotContain("@JsonCreator")
    assertThat(customer).doesNotContain("jackson")
  }

  @Test
  fun `validationAnnotations opt-in emits jakarta annotations and compiles against declared api`() {
    project.writeStandardBuild(
        modelMakerBlock = "modelmaker { features { validation { enabled = true } } }",
        dependencies = "implementation(\"jakarta.validation:jakarta.validation-api:3.1.1\")",
    )
    project.writeSchemas()

    val result = project.runner("compileJava").build()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    val java = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(java).contains("@NotNull(message = \"must not be null\")")
    assertThat(java).contains("import jakarta.validation.constraints.Pattern;")
    assertThat(java).doesNotContain("jackson")
  }

  @Test
  fun `openApi opt-in emits Schema annotations and compiles against declared swagger annotations`() {
    project.writeStandardBuild(
        modelMakerBlock = "modelmaker { features { openApi { enabled = true } } }",
        dependencies = "implementation(\"io.swagger.core.v3:swagger-annotations-jakarta:2.2.30\")",
    )
    project.write(
        "src/main/model/com.example.dto.Account.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Account",
        "description": "A billing account.", "type": "object",
        "required": ["id"],
        "properties": {
          "id":       { "type": "string", "description": "the account id", "example": "A-1" },
          "nickname": { "type": "string" }
        } }
      """,
    )

    val result = project.runner("compileJava").build()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    val java = project.generatedJava("com/example/dto/Account.java").readText()
    assertThat(java).contains("import io.swagger.v3.oas.annotations.media.Schema;")
    assertThat(java).contains("@Schema(description = \"A billing account.\")")
    assertThat(java)
        .contains(
            "@Schema(description = \"the account id\", example = \"A-1\"," +
                " requiredMode = Schema.RequiredMode.REQUIRED)"
        )
  }

  @Test
  fun `validation annotates getters by default and fields when annotateFields is set`() {
    project.writeStandardBuild(
        modelMakerBlock =
            "modelmaker { features { validation { enabled = true; annotateFields = true;" +
                " annotateGetters = false } } }",
        dependencies = "implementation(\"jakarta.validation:jakarta.validation-api:3.1.1\")",
    )
    project.writeSchemas()
    project.runner("compileJava").build()

    val java = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(java)
        .contains(
            "@Pattern(regexp = \"^C\", message = \"must match \\\"^C\\\"\")\n  private final String id;"
        )
    assertThat(java)
        .doesNotContain(
            "@Pattern(regexp = \"^C\", message = \"must match \\\"^C\\\"\")\n  public String getId()"
        )
  }

  @Test
  fun `does not add jspecify when the build already declares it`() {
    project.writeStandardBuild(dependencies = "implementation(\"org.jspecify:jspecify:1.0.0\")")
    project.writeSchemas()

    val classpath =
        project.runner("dependencies", "--configuration", "compileClasspath").build().output
    assertThat(classpath).contains("org.jspecify:jspecify:1.0.0")
    assertThat(classpath).doesNotContain("1.0.1")
  }

  @Test
  fun `preferPrimitives switches always-set scalars to primitives`() {
    project.writeStandardBuild(
        modelMakerBlock = "modelmaker { features { preferPrimitives { enabled = true } } }"
    )
    project.writeSchemas()

    project.runner("compileJava").build()

    val customer = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(customer).contains("private final int age;")
  }

  @Test
  fun `withers default off omits withXyz - opt-in emits them`() {
    project.writeStandardBuild()
    project.writeSchemas()
    project.runner("compileJava").build()
    assertThat(project.generatedJava("com/example/dto/Customer.java").readText())
        .doesNotContain("withHome")

    project.appendToBuildScript("modelmaker { features { withers { enabled = true } } }")
    project.runner("compileJava").build()
    assertThat(project.generatedJava("com/example/dto/Customer.java").readText())
        .contains("public Customer withHome(Address home) {")
  }

  @Test
  fun `a schema's features override applies per file, project default untouched`() {
    project.writeStandardBuild()
    project.writeSchemas()
    project.write(
        "src/main/model/com.example.dto.Widget.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Widget", "type": "object",
        "features": { "withers": { "enabled": true }, "preferPrimitives": { "enabled": true } },
        "required": ["count"], "properties": { "count": { "type": "integer" } } }
      """,
    )

    val result = project.runner("compileJava").build()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    // Overridden schema: withers + primitives on, even though both project defaults are off.
    val widget = project.generatedJava("com/example/dto/Widget.java").readText()
    assertThat(widget).contains("private final int count;")
    assertThat(widget).contains("public Widget withCount(int count) {")

    // Every other schema keeps the project defaults.
    val customer = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(customer).contains("private final Integer age;")
    assertThat(customer).doesNotContain("withAge")
  }

  @Test
  fun `kotlin extensions opt-in emits java dtos plus kotlin extensions`() {
    project.writeStandardBuild(
        languagePlugin = "id(\"org.jetbrains.kotlin.jvm\") version \"2.4.0\"",
        modelMakerBlock = "modelmaker { kotlin { enabled = true } }",
    )
    project.writeSchemas()

    val result = project.runner("compileKotlin").build()

    assertThat(result.task(":generateModelJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateModelKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    val java = project.generatedJava("com/example/dto/Customer.java")
    assertThat(java).exists()
    assertThat(java.readText()).contains("public final class Customer {")

    val ext = project.generatedKotlin("com/example/dto/CustomerExtensions.kt")
    assertThat(ext).exists()
    assertThat(ext.readText())
        .contains(
            "public inline fun Customer.mutate(block: Customer.BuilderMutator.() -> Unit): Customer ="
        )
  }

  @Test
  fun `generateModelJava is up-to-date then from-cache`() {
    // src { enabled = false }: `clean` only wipes build/, so restoring FROM_CACHE below needs the
    // default output to live there too - src.enabled's default output survives `clean` on purpose.
    project.writeStandardBuild(modelMakerBlock = "modelmaker { src { enabled = false } }")
    project.writeSchemas()

    project.runner("generateModelJava", "--build-cache").build()
    assertThat(
            project
                .runner("generateModelJava", "--build-cache")
                .build()
                .task(":generateModelJava")
                ?.outcome
        )
        .isEqualTo(TaskOutcome.UP_TO_DATE)

    project.runner("clean", "--build-cache").build()
    assertThat(
            project
                .runner("generateModelJava", "--build-cache")
                .build()
                .task(":generateModelJava")
                ?.outcome
        )
        .isEqualTo(TaskOutcome.FROM_CACHE)
  }

  @Test
  fun `builds without a schema directory at all`() {
    project.writeStandardBuild()

    val result = project.runner("build").build()

    assertThat(result.task(":generateModelJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":build")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(project.dir, "src/main/model/java")).doesNotExist()
  }

  @Test
  fun `generateModelJava writing into src is up-to-date on the second run`() {
    project.writeStandardBuild()
    project.writeSchemas()

    assertThat(project.runner("generateModelJava").build().task(":generateModelJava")?.outcome)
        .isEqualTo(TaskOutcome.SUCCESS)

    // The generated sources live under the schema directory, so they must not count as this task's
    // own input - otherwise the first run would dirty itself and execute again.
    assertThat(project.runner("generateModelJava").build().task(":generateModelJava")?.outcome)
        .isEqualTo(TaskOutcome.UP_TO_DATE)
  }

  @Test
  fun `generateModelJava writing into src restores from the build cache`() {
    project.writeStandardBuild()
    project.writeSchemas()

    project.runner("generateModelJava", "--build-cache").build()
    assertThat(project.generatedJava("com/example/dto/Customer.java")).exists()

    File(project.dir, "src/main/model/java").deleteRecursively()

    assertThat(
            project
                .runner("generateModelJava", "--build-cache")
                .build()
                .task(":generateModelJava")
                ?.outcome
        )
        .isEqualTo(TaskOutcome.FROM_CACHE)
    assertThat(project.generatedJava("com/example/dto/Customer.java")).exists()
  }

  @Test
  fun `generateModelKotlin generates nothing without kotlin extensions opt-in, then runs once enabled`() {
    project.writeStandardBuild(
        languagePlugin = "id(\"org.jetbrains.kotlin.jvm\") version \"2.4.0\""
    )
    project.writeSchemas()

    val first = project.runner("generateModelKotlin").build()
    assertThat(first.task(":generateModelKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedJava("com/example/dto/Address.java")).exists()
    assertThat(project.generatedKotlin("com/example/dto/AddressExtensions.kt")).doesNotExist()
    assertThat(File(projectDir, "src/main/model/kotlin")).doesNotExist()

    project.appendToBuildScript("modelmaker { kotlin { enabled = true } }")
    val second = project.runner("generateModelKotlin").build()
    assertThat(second.task(":generateModelKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedKotlin("com/example/dto/AddressExtensions.kt")).exists()
  }

  @Test
  fun `generateModelKotlin generates nothing without the kotlin jvm plugin, even with extensions on`() {
    project.writeStandardBuild(modelMakerBlock = "modelmaker { kotlin { enabled = true } }")
    project.writeSchemas()

    val result = project.runner("generateModelKotlin").build()
    assertThat(result.task(":generateModelKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedKotlin("com/example/dto/AddressExtensions.kt")).doesNotExist()
    assertThat(File(projectDir, "src/main/model/kotlin")).doesNotExist()
  }

  @Test
  fun `generateModelKotlin purges previously generated extensions once disabled`() {
    project.writeStandardBuild(
        languagePlugin = "id(\"org.jetbrains.kotlin.jvm\") version \"2.4.0\"",
        modelMakerBlock = "modelmaker { kotlin { enabled = true } }",
    )
    project.writeSchemas()

    project.runner("generateModelKotlin").build()
    assertThat(project.generatedKotlin("com/example/dto/AddressExtensions.kt")).exists()

    project.appendToBuildScript("modelmaker { kotlin { enabled = false } }")
    val result = project.runner("generateModelKotlin").build()
    assertThat(result.task(":generateModelKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedKotlin("com/example/dto/AddressExtensions.kt")).doesNotExist()
    // Not just empty - the directory itself is gone.
    assertThat(File(projectDir, "src/main/model/kotlin")).doesNotExist()
  }

  @Test
  fun `generateModelKotlin depends on generateModelJava`() {
    project.writeStandardBuild(
        languagePlugin = "id(\"org.jetbrains.kotlin.jvm\") version \"2.4.0\"",
        modelMakerBlock = "modelmaker { kotlin { enabled = true } }",
    )
    project.writeSchemas()

    val result = project.runner("generateModelKotlin").build()
    val tasks = result.tasks.map { it.path }
    assertThat(tasks.indexOf(":generateModelJava"))
        .isLessThan(tasks.indexOf(":generateModelKotlin"))
  }

  @Test
  fun `src disabled writes into build{ generated,sources,modelmaker} instead of src`() {
    project.writeStandardBuild(
        languagePlugin = "id(\"org.jetbrains.kotlin.jvm\") version \"2.4.0\"",
        modelMakerBlock = "modelmaker { kotlin { enabled = true }; src { enabled = false } }",
    )
    project.writeSchemas()

    val result = project.runner("compileKotlin").build()

    assertThat(result.task(":compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.buildJava("com/example/dto/Customer.java")).exists()
    assertThat(project.buildKotlin("com/example/dto/CustomerExtensions.kt")).exists()
    assertThat(project.generatedJava("com/example/dto/Customer.java")).doesNotExist()
    assertThat(project.generatedKotlin("com/example/dto/CustomerExtensions.kt")).doesNotExist()
  }

  @Test
  fun `contributes jspecify to the compile classpath`() {
    project.writeStandardBuild()
    project.writeSchemas()

    assertThat(project.runner("dependencies", "--configuration", "compileClasspath").build().output)
        .contains("org.jspecify:jspecify")
  }

  @Test
  fun `unresolved ref fails the build with a clear message`() {
    project.writeStandardBuild()
    project.write(
        "src/main/model/com.example.dto.Broken.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Broken", "type": "object",
        "properties": { "x": { "$ref": "com.example.dto.Missing" } } }
      """,
    )

    val failure = project.runner("generateModelJava").buildAndFail()

    assertThat(failure.output).contains("com.example.dto.Missing")
  }

  @Test
  fun `external $ref to a JDK type emits an import and compiles`() {
    project.writeStandardBuild()
    project.write(
        "src/main/model/com.example.dto.Event.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Event", "type": "object",
        "required": ["on"],
        "properties": {
          "on":  { "$ref": "java.time.LocalDate" },
          "at":  { "$ref": "java.time.Instant" }
        } }
      """,
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    val java = project.generatedJava("com/example/dto/Event.java").readText()
    assertThat(java).contains("import java.time.LocalDate;")
    assertThat(java).contains("import java.time.Instant;")
    assertThat(java).contains("private final LocalDate on;")
    assertThat(java).contains("@Nullable Instant at;")
  }

  @Test
  fun `external $ref gets @NotNull but no @Valid cascade under validation`() {
    project.writeStandardBuild(
        modelMakerBlock = "modelmaker { features { validation { enabled = true } } }",
        dependencies = "implementation(\"jakarta.validation:jakarta.validation-api:3.1.1\")",
    )
    project.write(
        "src/main/model/com.example.dto.Event.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Event", "type": "object",
        "required": ["on"],
        "properties": { "on": { "$ref": "java.time.LocalDate" } } }
      """,
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    val java = project.generatedJava("com/example/dto/Event.java").readText()
    assertThat(java).contains("@NotNull(message = \"must not be null\")")
    assertThat(java).contains("private final LocalDate on;")
    assertThat(java).doesNotContain("@Valid")
    assertThat(java).doesNotContain("import jakarta.validation.Valid;")
  }

  @Test
  fun `$ref to another schema resolves to that type with an @Valid cascade under validation`() {
    project.writeStandardBuild(
        modelMakerBlock = "modelmaker { features { validation { enabled = true } } }",
        dependencies = "implementation(\"jakarta.validation:jakarta.validation-api:3.1.1\")",
    )
    project.writeSchemas()

    val result = project.runner("compileJava").build()

    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    val customer = project.generatedJava("com/example/dto/Customer.java").readText()
    // Same package as Address, so the type is referenced by its simple name with no import.
    assertThat(customer).contains("private final Address home;")
    assertThat(customer).doesNotContain("import com.example.dto.Address;")
    assertThat(customer).contains("import jakarta.validation.Valid;")
    assertThat(customer).contains("@Valid")
  }

  @Test
  fun `avsc schemas alongside json schemas are picked up and compile together`() {
    project.writeStandardBuild()
    project.writeSchemas()
    project.write(
        "src/main/model/com.example.dto.Ticket.avsc",
        $$"""
      { "type": "record", "namespace": "com.example.dto", "name": "Ticket",
        "fields": [
          { "name": "id", "type": "string" },
          { "name": "priority", "type": { "type": "enum", "name": "Priority", "symbols": ["LOW", "HIGH"] } },
          { "name": "note", "type": ["null", "string"], "default": null }
        ] }
      """,
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":generateModelJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    val ticket = project.generatedJava("com/example/dto/Ticket.java").readText()
    assertThat(ticket).contains("public final class Ticket {")
    assertThat(ticket).doesNotContain("enum Priority")
    assertThat(ticket).contains("private final String priority;")
  }

  @Test
  fun `$ref to another schema needs no import and compiles without validation`() {
    project.writeStandardBuild()
    project.writeSchemas()

    val result = project.runner("compileJava").build()

    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    val customer = project.generatedJava("com/example/dto/Customer.java").readText()
    assertThat(customer).contains("private final Address home;")
    assertThat(customer).doesNotContain("@Valid")
  }
}
