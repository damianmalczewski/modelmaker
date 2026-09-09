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

package io.github.malczuuu.modelmaker.gradle

import io.github.malczuuu.modelmaker.gradle.dsl.ModelMakerExtension
import io.github.malczuuu.modelmaker.gradle.tasks.JavaModelGenerate
import io.github.malczuuu.modelmaker.gradle.tasks.KotlinModelGenerate
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Generates immutable Java models from a lightweight JSON-Schema subset, or from Avro `.avsc`
 * record schemas:
 *
 * 1. Registers the `modelmaker { }` extension.
 * 2. Registers a `generateModelJava` task that compiles every `*.json` and `*.avsc` under
 *    `src/main/model` into `.java` models, and a `generateModelKotlin` (depending on
 *    `generateModelJava`) that writes Kotlin extensions.
 * 3. Registers output dirs as source directories of the `main` source set, built by their
 *    respective tasks (`javac` picks up the `.java`; the Kotlin plugin, if present, the `.kt`).
 *
 * The plugin contributes `org.jspecify:jspecify` on `compileOnly`, if the build script has not
 * already declared it.
 */
public class ModelMakerPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("modelmaker", ModelMakerExtension::class.java)

      extension.features.jackson.convention(false)
      extension.features.validation.convention(false)
      extension.features.withers.convention(false)
      extension.features.preferPrimitives.convention(false)
      extension.features.openapi.convention(false)
      extension.kotlin.enabled.convention(false)
      extension.src.enabled.convention(true)

      val kotlinPluginApplied = objects.property(Boolean::class.java).convention(false)
      pluginManager.withPlugin(KOTLIN_JVM_PLUGIN_ID) { kotlinPluginApplied.set(true) }

      val schemaSourceDirectory = layout.projectDirectory.dir("src/main/model")

      val javaOutputDir =
          extension.src.enabled.flatMap { enabled ->
            if (enabled) providers.provider { layout.projectDirectory.dir("src/main/model/java") }
            else layout.buildDirectory.dir("generated/sources/modelmaker/java/main")
          }
      val kotlinOutputDir =
          extension.src.enabled.flatMap { enabled ->
            if (enabled) {
              providers.provider { layout.projectDirectory.dir("src/main/model/kotlin") }
            } else layout.buildDirectory.dir("generated/sources/modelmaker/kotlin/main")
          }

      val generateModelJava =
          tasks.register<JavaModelGenerate>("generateModelJava") {
            group = "modelmaker"
            description = "Generates Java model from JSON specification files"

            sourceDirectory.set(schemaSourceDirectory)
            javaOutputDirectory.set(javaOutputDir)

            inheritFeatures(extension.features)
          }

      val generateModelKotlin =
          tasks.register<KotlinModelGenerate>("generateModelKotlin") {
            group = "modelmaker"
            description = "Generates Kotlin extensions from JSON specification files"
            dependsOn(generateModelJava)

            sourceDirectory.set(schemaSourceDirectory)
            kotlinOutputDirectory.set(kotlinOutputDir)

            inheritKotlin(extension.kotlin.enabled)
            this.kotlinPluginApplied.set(kotlinPluginApplied)
          }

      plugins.withType<JavaPlugin> {
        extensions.configure<SourceSetContainer> {
          named("main") {
            java.srcDir(generateModelJava.flatMap { task -> task.javaOutputDirectory })
            java.srcDir(generateModelKotlin.flatMap { task -> task.kotlinOutputDirectory })
          }
        }
        addJspecifyUnlessDeclared(project)
      }
    }
  }

  /** The generated code is `@NullMarked`; add JSpecify unless the build already provides it. */
  private fun addJspecifyUnlessDeclared(project: Project) {
    val configurations = project.configurations
    fun hasJspecify(deps: Iterable<Dependency>) = deps.any {
      it.group == JSPECIFY_GROUP && it.name == JSPECIFY_NAME
    }

    configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).configure {
      withDependencies {
        val declared =
            hasJspecify(this) ||
                CANDIDATE_CONFIGURATIONS.any { name ->
                  configurations.findByName(name)?.let { hasJspecify(it.dependencies) } == true
                }
        if (!declared) {
          add(project.dependencies.create("$JSPECIFY_GROUP:$JSPECIFY_NAME:$JSPECIFY_VERSION"))
        }
      }
    }
  }

  private companion object {
    const val JSPECIFY_GROUP = "org.jspecify"
    const val JSPECIFY_NAME = "jspecify"
    const val JSPECIFY_VERSION = "1.0.1"
    const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"

    val CANDIDATE_CONFIGURATIONS =
        listOf(
            JavaPlugin.API_CONFIGURATION_NAME,
            JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME,
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
        )
  }
}
