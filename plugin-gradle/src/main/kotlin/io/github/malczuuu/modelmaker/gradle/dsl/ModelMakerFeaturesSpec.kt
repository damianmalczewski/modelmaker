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

package io.github.malczuuu.modelmaker.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.tasks.Nested

/**
 * The generation features, configured via `modelmaker { features { } }`. Each feature has its own
 * spec type so it can grow independent options later.
 *
 * ```
 * features {
 *     jackson    { enabled = true; annotateFields = false; annotateGetters = true }
 *     validation { enabled = true }
 *     openApi    { enabled = true }
 *     withers    { enabled = false }
 *     preferPrimitives { enabled = false }
 * }
 * ```
 *
 * `jackson` / `validation` / `openApi` carry on/off plus field / getter placement; `withers` /
 * `preferPrimitives` are plain on/off. Every flag defaults to `false` except each annotation
 * feature's `annotateGetters` (defaults to `true`), and every flag can be overridden per schema
 * through a top-level `"features"` object in the schema file.
 *
 * @property jackson Jackson annotations (`@JsonCreator`, `@JsonProperty`, ...).
 * @property validation `jakarta.validation` constraint annotations and `@Valid` cascades.
 * @property openApi OpenAPI `@Schema` annotations (`io.swagger.v3.oas.annotations.media.Schema`).
 * @property withers a `withXyz(value)` single-field copy method per property.
 * @property preferPrimitives always-set scalar fields use the primitive type (`int`, `double`,
 *   `boolean`) instead of the boxed default.
 */
public abstract class ModelMakerFeaturesSpec {

  @get:Nested public abstract val jackson: ModelMakerJacksonSpec

  @get:Nested public abstract val validation: ModelMakerValidationSpec

  @get:Nested public abstract val openApi: ModelMakerOpenApiSpec

  @get:Nested public abstract val withers: ModelMakerWithersSpec

  @get:Nested public abstract val preferPrimitives: ModelMakerPreferPrimitivesSpec

  /**
   * Configures the Jackson annotations.
   *
   * @param configuration action applied to [jackson]
   */
  public fun jackson(configuration: Action<in ModelMakerJacksonSpec>) {
    configuration.execute(jackson)
  }

  /**
   * Configures the `jakarta.validation` annotations.
   *
   * @param configuration action applied to [validation]
   */
  public fun validation(configuration: Action<in ModelMakerValidationSpec>) {
    configuration.execute(validation)
  }

  /**
   * Configures the OpenAPI `@Schema` annotations.
   *
   * @param configuration action applied to [openApi]
   */
  public fun openApi(configuration: Action<in ModelMakerOpenApiSpec>) {
    configuration.execute(openApi)
  }

  /**
   * Configures the `withXyz(value)` copy methods.
   *
   * @param configuration action applied to [withers]
   */
  public fun withers(configuration: Action<in ModelMakerWithersSpec>) {
    configuration.execute(withers)
  }

  /**
   * Configures the primitive-vs-boxed scalar preference.
   *
   * @param configuration action applied to [preferPrimitives]
   */
  public fun preferPrimitives(configuration: Action<in ModelMakerPreferPrimitivesSpec>) {
    configuration.execute(preferPrimitives)
  }
}
