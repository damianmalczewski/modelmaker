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

/**
 * The generation features, configured via `modelmaker { features { } }`. Every flag defaults to
 * `false` and can be overridden per schema through a top-level `"features"` object in the schema
 * file.
 *
 * @property jackson emit Jackson annotations on the models.
 * @property validation emit `jakarta.validation` annotations on the models.
 * @property withers emit a `withXyz(value)` single-field copy method per property.
 * @property preferPrimitives always-set scalar fields (`integer`, `number`, `boolean`) use the
 *   primitive type (`int`, `double`, `boolean`) instead of the boxed default.
 */
public abstract class ModelMakerFeaturesSpec {

  public abstract val jackson: Property<Boolean>

  public abstract val validation: Property<Boolean>

  public abstract val withers: Property<Boolean>

  public abstract val preferPrimitives: Property<Boolean>
}
