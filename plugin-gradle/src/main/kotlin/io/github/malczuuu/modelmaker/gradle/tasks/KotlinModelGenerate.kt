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

import io.github.malczuuu.modelmaker.KotlinExtensionMaker
import io.github.malczuuu.modelmaker.SchemaLoaders
import io.github.malczuuu.modelmaker.gradle.dsl.ModelMakerKotlinSpec
import java.io.File
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
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
 * and writes one `<Name>Extensions.kt` per schema into [kotlinOutputDirectory]: a `mutate { }`
 * extension function over the corresponding Java DTO's `Builder` (see [JavaModelGenerate]).
 *
 * When [generationEnabled] is on and the Kotlin JVM plugin is applied to the project it (re)writes
 * every extensions file, otherwise it purges [kotlinOutputDirectory] and stops - so switching
 * `kotlin.enabled` off, or removing the Kotlin JVM plugin, cleans up previously generated `.kt`
 * files rather than leaving them stale.
 *
 * Not incremental: [kotlinOutputDirectory] is wiped on every run, rebuilt only when generating.
 * Cacheable, with [sourceDirectory] tracked relative, so a cache hit restores the whole output
 * without re-emitting.
 *
 * @param objects factory creating this task's `kotlin { }` block
 */
@CacheableTask
public abstract class KotlinModelGenerate @Inject constructor(objects: ObjectFactory) :
    DefaultTask() {

  /** Directory scanned recursively for `*.json` and `*.avsc` schema files. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public abstract val sourceDirectory: DirectoryProperty

  /**
   * Where the generated Kotlin extensions are written; cleared before each run, and left absent
   * (not just empty) when generation is disabled.
   */
  @get:OutputDirectory public abstract val kotlinOutputDirectory: DirectoryProperty

  private val kotlinSpec: ModelMakerKotlinSpec = objects.newInstance()

  /**
   * Whether this task generates, defaulting to `modelmaker { kotlin { } }`'s `enabled` value (see
   * [inheritKotlin]) unless overridden through [kotlin]. Named `generationEnabled`, not `enabled`,
   * to not collide with [org.gradle.api.Task.getEnabled].
   */
  @get:Input
  public val generationEnabled: Provider<Boolean>
    get() = kotlinSpec.enabled

  /**
   * Configures Kotlin-specific generation, overriding the `modelmaker { kotlin { } }` defaults for
   * this task only, see [ModelMakerKotlinSpec]:
   * ```
   * tasks.named<KotlinModelGenerate>("generateModelKotlin") {
   *   kotlin {
   *     enabled = false
   *   }
   * }
   * ```
   *
   * @param configuration action applied to the Kotlin generation options
   */
  public fun kotlin(configuration: Action<in ModelMakerKotlinSpec>) {
    configuration.execute(kotlinSpec)
  }

  /**
   * Defaults this task's `enabled` flag to the `modelmaker { kotlin { } }` one. Called by the
   * plugin right after the task is registered, so an explicit [kotlin] call on the task always
   * overrides it.
   *
   * @param enabled `modelmaker.kotlin.enabled`
   */
  internal fun inheritKotlin(enabled: Provider<Boolean>) {
    kotlinSpec.enabled.convention(enabled)
  }

  /**
   * Whether the Kotlin JVM plugin is applied to the project. Not part of the `kotlin { }` DSL -
   * it's a fact about the project, not a user toggle - so it's set once by the plugin
   * (`ModelMakerPlugin`) rather than defaulted/overridden like [generationEnabled].
   */
  @get:Input public abstract val kotlinPluginApplied: Property<Boolean>

  @TaskAction
  public fun generate() {
    val kotlinDir = kotlinOutputDirectory.get().asFile

    // Gradle pre-creates @OutputDirectory properties as empty directories before this action runs;
    // deleting it here without recreating leaves nothing on disk once the task finishes, rather
    // than an empty directory.
    kotlinDir.deleteRecursively()

    if (!generationEnabled.getOrElse(false) || !kotlinPluginApplied.getOrElse(false)) {
      logger.debug("Kotlin extensions generation disabled; removed {}", kotlinDir)
      return
    }

    kotlinDir.mkdirs()

    val sourceDir = sourceDirectory.get().asFile
    val schemas =
        sourceDir
            .walkTopDown()
            .filter { it.isFile && (it.extension == "json" || it.extension == "avsc") }
            .toList()
    if (schemas.isEmpty()) {
      logger.lifecycle("No model schemas found in {}", sourceDir)
      return
    }

    val kotlinExtensionMaker = KotlinExtensionMaker()

    SchemaLoaders.createDelegatingSchemaLoader().load(schemas.map { it.toPath() }).forEach { type ->
      write(
          kotlinDir,
          type.packageName,
          "${type.name}Extensions.kt",
          kotlinExtensionMaker.emit(type),
      )
      logger.debug("Generated {}.{}Extensions", type.packageName, type.name)
    }
  }

  private fun write(root: File, packageName: String, fileName: String, code: String) {
    val dir = root.resolve(packageName.replace('.', '/'))
    dir.mkdirs()
    dir.resolve(fileName).writeText(code)
  }
}
