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
 * Per-schema override of a {@link JacksonConfig}, parsed from a schema file's top-level {@code
 * "features"} object. Each flag is {@code null} when the schema does not set it - {@link
 * JacksonConfig#withOverride} then keeps the project default.
 */
public final class JacksonOverride {

  private static final JacksonOverride NONE = new JacksonOverride(null, null, null, null);

  private final @Nullable Boolean enabled;
  private final @Nullable Boolean annotateFields;
  private final @Nullable Boolean annotateGetters;
  private final @Nullable Boolean includeNonNull;

  /**
   * Creates a new override with the given flags; pass {@code null} for a flag to leave it unset.
   *
   * @param enabled the {@code enabled} override, or {@code null}.
   * @param annotateFields the {@code annotateFields} override, or {@code null}.
   * @param annotateGetters the {@code annotateGetters} override, or {@code null}.
   */
  JacksonOverride(
      @Nullable Boolean enabled,
      @Nullable Boolean annotateFields,
      @Nullable Boolean annotateGetters) {
    this(enabled, annotateFields, annotateGetters, null);
  }

  /**
   * Creates a new override.
   *
   * @param enabled the {@code enabled} flag, or {@code null} to keep the project default.
   * @param annotateFields the {@code annotateFields} flag, or {@code null}.
   * @param annotateGetters the {@code annotateGetters} flag, or {@code null}.
   * @param includeNonNull the {@code includeNonNull} flag, or {@code null}.
   */
  JacksonOverride(
      @Nullable Boolean enabled,
      @Nullable Boolean annotateFields,
      @Nullable Boolean annotateGetters,
      @Nullable Boolean includeNonNull) {
    this.enabled = enabled;
    this.annotateFields = annotateFields;
    this.annotateGetters = annotateGetters;
    this.includeNonNull = includeNonNull;
  }

  /**
   * No overrides - every flag falls through to the project default.
   *
   * @return the shared empty instance.
   */
  static JacksonOverride none() {
    return NONE;
  }

  /**
   * An override that sets only {@code enabled}, leaving placement to the project default.
   *
   * @param value the {@code enabled} override.
   * @return the override.
   */
  static JacksonOverride enabled(boolean value) {
    return new JacksonOverride(value, null, null, null);
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
   * The {@code includeNonNull} flag, when the schema set it.
   *
   * @return the flag, or empty to keep the project default.
   */
  public Optional<Boolean> getIncludeNonNull() {
    return Optional.ofNullable(includeNonNull);
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
    if (!(obj instanceof JacksonOverride other)) {
      return false;
    }
    return Objects.equals(enabled, other.enabled)
        && Objects.equals(annotateFields, other.annotateFields)
        && Objects.equals(annotateGetters, other.annotateGetters)
        && Objects.equals(includeNonNull, other.includeNonNull);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, annotateFields, annotateGetters, includeNonNull);
  }

  @Override
  public String toString() {
    return "JacksonOverride["
        + ("enabled=" + enabled)
        + (", annotateFields=" + annotateFields)
        + (", annotateGetters=" + annotateGetters)
        + (", includeNonNull=" + includeNonNull)
        + "]";
  }
}
