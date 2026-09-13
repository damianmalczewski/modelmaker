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
 * Where generated sources are written, configured via `modelmaker { src { } }`.
 *
 * @property enabled when `true`, the generated sources are placed into the source tree, in the
 *   `java` and `kotlin` subdirectories of the schema directory, where they are meant to be reviewed
 *   and committed; when `false`, they are written to
 *   `build/generated/sources/modelmaker/{java,kotlin}/main` instead. Either way the directory is
 *   wiped and rebuilt on every run, so it is never hand-edited.
 */
public abstract class ModelMakerSrcSpec {

  public abstract val enabled: Property<Boolean>
}
