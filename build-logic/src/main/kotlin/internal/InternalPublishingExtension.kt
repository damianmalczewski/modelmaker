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

package internal

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Artifact-level publishing metadata, configured lazily from build scripts.
 *
 * @constructor Injected by Gradle via [ObjectFactory].
 */
abstract class InternalPublishingExtension @Inject constructor(objects: ObjectFactory) {

  /** Human-readable display name of the artifact, used in the generated POM. */
  val displayName: Property<String> = objects.property(String::class.java)

  /** Description of the artifact, used in the generated POM. */
  val description: Property<String> = objects.property(String::class.java)
}
