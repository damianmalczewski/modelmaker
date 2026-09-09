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

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Per-schema override of a {@link ValidationConfig}, parsed from a schema file's top-level {@code
 * "features"} object. Each flag is {@code null} when the schema does not set it - {@link
 * ValidationConfig#withOverride} then keeps the project default. Created through {@link #none()} /
 * {@link #of} / {@link #enabled(boolean)}.
 */
public final class ValidationOverride {

  private static final ValidationOverride NONE = new ValidationOverride(null, null, null);

  private final @Nullable Boolean enabled;
  private final @Nullable Boolean annotateFields;
  private final @Nullable Boolean annotateGetters;

  ValidationOverride(
      @Nullable Boolean enabled,
      @Nullable Boolean annotateFields,
      @Nullable Boolean annotateGetters) {
    this.enabled = enabled;
    this.annotateFields = annotateFields;
    this.annotateGetters = annotateGetters;
  }

  /**
   * No overrides - every flag falls through to the project default.
   *
   * @return the shared empty instance.
   */
  static ValidationOverride none() {
    return NONE;
  }

  /**
   * An override with the given flags; pass {@code null} for a flag to leave it unset.
   *
   * @param enabled the {@code enabled} override, or {@code null}.
   * @param annotateFields the {@code annotateFields} override, or {@code null}.
   * @param annotateGetters the {@code annotateGetters} override, or {@code null}.
   * @return the override.
   */
  static ValidationOverride of(
      @Nullable Boolean enabled,
      @Nullable Boolean annotateFields,
      @Nullable Boolean annotateGetters) {
    return new ValidationOverride(enabled, annotateFields, annotateGetters);
  }

  /**
   * An override that sets only {@code enabled}, leaving placement to the project default.
   *
   * @param value the {@code enabled} override.
   * @return the override.
   */
  static ValidationOverride enabled(boolean value) {
    return new ValidationOverride(value, null, null);
  }

  /**
   * The {@code enabled} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(enabled);
  }

  /**
   * The {@code annotateFields} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getAnnotateFields() {
    return Optional.ofNullable(annotateFields);
  }

  /**
   * The {@code annotateGetters} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getAnnotateGetters() {
    return Optional.ofNullable(annotateGetters);
  }

  /**
   * Whether this override sets nothing at all.
   *
   * @return {@code true} when every flag is unset.
   */
  public boolean isEmpty() {
    return enabled == null && annotateFields == null && annotateGetters == null;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ValidationOverride other)) {
      return false;
    }
    return Objects.equals(enabled, other.enabled)
        && Objects.equals(annotateFields, other.annotateFields)
        && Objects.equals(annotateGetters, other.annotateGetters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, annotateFields, annotateGetters);
  }

  @Override
  public String toString() {
    return "ValidationOverride["
        + ("enabled=" + enabled)
        + (", annotateFields=" + annotateFields)
        + (", annotateGetters=" + annotateGetters)
        + "]";
  }
}
