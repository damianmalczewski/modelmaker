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

package io.github.malczuuu.modelmaker.fixtures

import java.io.File
import org.gradle.testkit.runner.GradleRunner

/**
 * Temporary Gradle project driven through [GradleRunner] for the `io.github.malczuuu.modelmaker`
 * plugin.
 */
class TestProject(val dir: File) {

  fun write(relativePath: String, content: String): File =
      File(dir, relativePath).apply {
        parentFile.mkdirs()
        writeText(content.trimIndent().trim() + "\n")
      }

  fun appendToBuildScript(content: String) {
    File(dir, "build.gradle.kts").appendText("\n" + content.trimIndent().trim() + "\n")
  }

  fun writeStandardBuild(
      languagePlugin: String = "java",
      modelMakerBlock: String = "",
      dependencies: String = "",
  ) {
    write("settings.gradle.kts", "rootProject.name = \"under-test\"")
    write(
        "build.gradle.kts",
        """
      plugins {
          $languagePlugin
          id("io.github.malczuuu.modelmaker")
      }

      repositories {
          mavenCentral()
      }

      $modelMakerBlock

      dependencies {
          $dependencies
      }
      """,
    )
    write(
        "gradle.properties",
        """
      org.gradle.jvmargs=-Xmx512m
      org.gradle.daemon.idletimeout=10000
      """,
    )
  }

  fun writeSchemas() {
    write(
        "src/main/model/com.example.dto.Address.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Address", "type": "object",
        "required": ["city"],
        "properties": { "city": { "type": "string", "minLength": 1 } } }
      """,
    )
    write(
        "src/main/model/com.example.dto.Customer.json",
        $$"""
      { "$modelmaker": "v1.0", "title": "com.example.dto.Customer", "type": "object",
        "required": ["id", "age", "home"],
        "properties": {
          "id":   { "type": "string", "pattern": "^C" },
          "age":  { "type": "integer", "minimum": 0 },
          "home": { "$ref": "com.example.dto.Address" }
        } }
      """,
    )
  }

  fun runner(vararg arguments: String): GradleRunner =
      GradleRunner.create().withProjectDir(dir).withPluginClasspath().withArguments(*arguments)

  /** Where `generateModelJava` writes by default (`modelmaker.src.enabled` defaults `true`). */
  fun generatedJava(relativePath: String): File = File(dir, "src/main/model/java/$relativePath")

  /** Where `generateModelKotlin` writes by default (`modelmaker.src.enabled` defaults `true`). */
  fun generatedKotlin(relativePath: String): File = File(dir, "src/main/model/kotlin/$relativePath")

  /** Where `generateModelJava` writes with `modelmaker { src { enabled = false } }`. */
  fun buildJava(relativePath: String): File =
      File(dir, "build/generated/sources/modelmaker/java/main/$relativePath")

  /** Where `generateModelKotlin` writes with `modelmaker { src { enabled = false } }`. */
  fun buildKotlin(relativePath: String): File =
      File(dir, "build/generated/sources/modelmaker/kotlin/main/$relativePath")
}
