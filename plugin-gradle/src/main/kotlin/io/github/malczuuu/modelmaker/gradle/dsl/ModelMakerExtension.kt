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

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

/**
 * DSL of the `io.github.malczuuu.modelmaker` plugin, registered as `modelmaker { }` on the project.
 *
 * ```
 * modelmaker {
 *   src {
 *     enabled = true | false
 *   }
 *   kotlin {
 *     enabled = true | false
 *   }
 *   features {
 *     jackson          = true | false
 *     validation       = true | false
 *     withers          = true | false
 *     preferPrimitives = true | false
 *   }
 * }
 * ```
 *
 * @param objects factory creating the [src], [kotlin], and [features] blocks
 */
public abstract class ModelMakerExtension @Inject constructor(objects: ObjectFactory) {

  /** Where generated sources are written, see [ModelMakerSrcSpec]. */
  internal val src: ModelMakerSrcSpec = objects.newInstance()

  /**
   * Configures where generated sources are written, see [ModelMakerSrcSpec]:
   * ```
   * modelmaker {
   *   src {
   *     enabled = true | false
   *   }
   * }
   * ```
   *
   * @param configuration action applied to the source-output options
   */
  public fun src(configuration: Action<in ModelMakerSrcSpec>) {
    configuration.execute(src)
  }

  /** Kotlin-specific generation, see [ModelMakerKotlinSpec]. */
  internal val kotlin: ModelMakerKotlinSpec = objects.newInstance()

  /**
   * Configures Kotlin-specific generation, see [ModelMakerKotlinSpec]:
   * ```
   * modelmaker {
   *   kotlin {
   *     enabled = true | false
   *   }
   * }
   * ```
   *
   * Unlike the [features] flags, `kotlin.enabled` is project-wide only - it cannot be overridden
   * per schema.
   *
   * @param configuration action applied to the Kotlin generation options
   */
  public fun kotlin(configuration: Action<in ModelMakerKotlinSpec>) {
    configuration.execute(kotlin)
  }

  /** Which generation features are on, see [ModelMakerFeaturesSpec]. */
  internal val features: ModelMakerFeaturesSpec = objects.newInstance()

  /**
   * Configures which generation features are on, see [ModelMakerFeaturesSpec]:
   * ```
   * modelmaker {
   *   features {
   *     jackson          = true | false
   *     validation       = true | false
   *     withers          = true | false
   *     preferPrimitives = true | false
   *     openapi          = true | false
   *   }
   * }
   * ```
   *
   * Every flag can also be overridden per schema through a top-level `"features"` object in the
   * schema file.
   *
   * @param configuration action applied to the feature flags
   */
  public fun features(configuration: Action<in ModelMakerFeaturesSpec>) {
    configuration.execute(features)
  }
}
