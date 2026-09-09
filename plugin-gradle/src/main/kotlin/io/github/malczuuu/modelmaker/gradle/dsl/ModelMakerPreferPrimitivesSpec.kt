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
 * The `preferPrimitives` feature - a plain on/off toggle.
 *
 * ```
 * preferPrimitives {
 *     enabled = true | false
 * }
 * ```
 *
 * Default: `enabled = false`.
 *
 * @property enabled whether the feature is on.
 */
public abstract class ModelMakerPreferPrimitivesSpec {

  @get:Input public abstract val enabled: Property<Boolean>
}
