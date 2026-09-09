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

package io.github.malczuuu.modelmaker.gradle.tasks

import io.github.malczuuu.modelmaker.JavaModelMaker
import io.github.malczuuu.modelmaker.ModelOptions
import io.github.malczuuu.modelmaker.SchemaLoaders
import io.github.malczuuu.modelmaker.gradle.dsl.ModelMakerFeaturesSpec
import java.io.File
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.newInstance

/**
 * Reads the lightweight JSON schema files (and Avro `.avsc` record schemas) under [sourceDirectory]
 * and writes one immutable Java DTO per schema into [javaOutputDirectory].
 *
 * Not incremental: [javaOutputDirectory] is wiped and rebuilt on every run. Cacheable, with
 * [sourceDirectory] tracked relative, so a cache hit restores the whole output without re-emitting.
 *
 * @param objects factory creating this task's `features { }` block
 */
@CacheableTask
public abstract class JavaModelGenerate @Inject constructor(objects: ObjectFactory) :
    DefaultTask() {

  /** Directory scanned recursively for `*.json` and `*.avsc` schema files. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public abstract val sourceDirectory: DirectoryProperty

  /** Where the generated Java DTOs are written; cleared before each run. */
  @get:OutputDirectory public abstract val javaOutputDirectory: DirectoryProperty

  private val featuresSpec: ModelMakerFeaturesSpec = objects.newInstance()

  /**
   * The Java-side feature flags this task emits with, defaulting to the `modelmaker { features { }
   * }` values (see [inheritFeatures]) unless overridden through [features]. Each is also
   * overridable per schema via a `"features"` object in the schema file.
   */
  @get:Input
  public val jackson: Provider<Boolean>
    get() = featuresSpec.jackson

  @get:Input
  public val validation: Provider<Boolean>
    get() = featuresSpec.validation

  @get:Input
  public val withers: Provider<Boolean>
    get() = featuresSpec.withers

  @get:Input
  public val preferPrimitives: Provider<Boolean>
    get() = featuresSpec.preferPrimitives

  @get:Input
  public val openapi: Provider<Boolean>
    get() = featuresSpec.openapi

  /**
   * Configures which features this task emits, overriding the `modelmaker { features { } }`
   * defaults for this task only, see [ModelMakerFeaturesSpec]:
   * ```
   * tasks.named<JavaModelGenerate>("generateModelJava") {
   *   features {
   *     validation = false
   *   }
   * }
   * ```
   *
   * @param configuration action applied to the feature flags
   */
  public fun features(configuration: Action<in ModelMakerFeaturesSpec>) {
    configuration.execute(featuresSpec)
  }

  /**
   * Defaults this task's feature flags to the `modelmaker { features { } }` ones. Called by the
   * plugin right after the task is registered, so an explicit [features] call on the task always
   * overrides it.
   *
   * @param features the `modelmaker { features { } }` spec
   */
  internal fun inheritFeatures(features: ModelMakerFeaturesSpec) {
    featuresSpec.jackson.convention(features.jackson)
    featuresSpec.validation.convention(features.validation)
    featuresSpec.withers.convention(features.withers)
    featuresSpec.preferPrimitives.convention(features.preferPrimitives)
    featuresSpec.openapi.convention(features.openapi)
  }

  @TaskAction
  public fun generate() {
    val sourceDir = sourceDirectory.get().asFile
    val javaDir = javaOutputDirectory.get().asFile

    javaDir.deleteRecursively()
    javaDir.mkdirs()

    val schemas =
        sourceDir
            .walkTopDown()
            .filter { it.isFile && (it.extension == "json" || it.extension == "avsc") }
            .toList()
    if (schemas.isEmpty()) {
      logger.lifecycle("No model schemas found in {}", sourceDir)
      return
    }

    val javaModelMaker =
        JavaModelMaker(
            ModelOptions.builder()
                .preferPrimitives(preferPrimitives.get())
                .withers(withers.get())
                .jackson(jackson.get())
                .validation(validation.get())
                .openapi(openapi.get())
                .build()
        )

    SchemaLoaders.createDelegatingSchemaLoader().load(schemas.map { it.toPath() }).forEach { type ->
      write(javaDir, type.packageName, "${type.name}.java", javaModelMaker.emit(type))
      logger.debug("Generated {}.{}", type.packageName, type.name)
    }
  }

  private fun write(root: File, packageName: String, fileName: String, code: String) {
    val dir = root.resolve(packageName.replace('.', '/'))
    dir.mkdirs()
    dir.resolve(fileName).writeText(code)
  }
}
