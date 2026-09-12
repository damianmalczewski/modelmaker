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

package io.github.malczuuu.modelmaker;

import org.jspecify.annotations.Nullable;

/** Builds {@link Property} instances for tests. */
final class PropertyFactory {

  private PropertyFactory() {}

  /** A property without OpenAPI metadata. */
  static Property property(
      String name,
      PropType type,
      boolean required,
      Constraints constraints,
      String jsonName,
      @Nullable DefaultValue defaultValue) {
    return property(name, type, required, constraints, jsonName, defaultValue, null, null);
  }

  /** A property with OpenAPI metadata. */
  static Property property(
      String name,
      PropType type,
      boolean required,
      Constraints constraints,
      String jsonName,
      @Nullable DefaultValue defaultValue,
      @Nullable String description,
      @Nullable String example) {
    return new Property(
        name, type, required, constraints, jsonName, defaultValue, description, example, false);
  }

  /** A property masked in {@code toString()}. */
  static Property sensitiveProperty(
      String name,
      PropType type,
      boolean required,
      Constraints constraints,
      String jsonName,
      @Nullable DefaultValue defaultValue) {
    return new Property(
        name, type, required, constraints, jsonName, defaultValue, null, null, true);
  }
}
