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

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * The `jackson` feature: whether its annotations are emitted, and where they sit.
 *
 * ```
 * jackson {
 *     enabled         = true | false
 *     annotateFields  = true | false
 *     annotateGetters = true | false
 *     includeNonNull  = true | false
 * }
 * ```
 *
 * Defaults: `enabled = false`, `annotateFields = false`, `annotateGetters = true`, `includeNonNull
 * = false`. With both `annotateFields` and `annotateGetters` on, the annotations are emitted on
 * both.
 *
 * @property enabled whether the feature emits any annotations.
 * @property annotateFields place the annotations on the generated fields.
 * @property annotateGetters place the annotations on the generated getters.
 * @property includeNonNull emit `@JsonInclude(JsonInclude.Include.NON_NULL)` on the generated
 *   class, so a property left unset is omitted from the serialized JSON instead of written as
 *   `null`.
 */
public abstract class ModelMakerJacksonSpec {

  @get:Input public abstract val enabled: Property<Boolean>

  @get:Input public abstract val annotateFields: Property<Boolean>

  @get:Input public abstract val annotateGetters: Property<Boolean>

  @get:Input public abstract val includeNonNull: Property<Boolean>
}
