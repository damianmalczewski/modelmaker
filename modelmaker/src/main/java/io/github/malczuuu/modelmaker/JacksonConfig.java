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
import org.jspecify.annotations.Nullable;

/**
 * Jackson annotation configuration: whether the feature is on, and whether its annotations sit on
 * the generated fields, the getters, or both. Built through {@link #builder()}; read back through
 * {@code ModelOptions}.
 */
public final class JacksonConfig {

  private static final JacksonConfig DEFAULT_VALUE = new JacksonConfig(false, false, true);

  private final boolean enabled;
  private final boolean annotateFields;
  private final boolean annotateGetters;

  private JacksonConfig(boolean enabled, boolean annotateFields, boolean annotateGetters) {
    this.enabled = enabled;
    this.annotateFields = annotateFields;
    this.annotateGetters = annotateGetters;
  }

  /**
   * The default configuration - disabled, getter placement ({@code enabled=false}, {@code
   * annotateFields=false}, {@code annotateGetters=true}).
   *
   * @return the shared default instance.
   */
  static JacksonConfig defaults() {
    return DEFAULT_VALUE;
  }

  /**
   * Starts a new builder, pre-set to the feature defaults (disabled, getter placement).
   *
   * @return a new {@link Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Whether the feature is on.
   *
   * @return {@code true} when on.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Whether the annotations sit on the generated fields.
   *
   * @return {@code true} when they do.
   */
  public boolean isAnnotateFields() {
    return annotateFields;
  }

  /**
   * Whether the annotations sit on the generated getters.
   *
   * @return {@code true} when they do.
   */
  public boolean isAnnotateGetters() {
    return annotateGetters;
  }

  /**
   * Whether the feature emits annotations on the fields - on and placed there.
   *
   * @return {@code true} when field annotations should be emitted.
   */
  public boolean emitsOnFields() {
    return enabled && annotateFields;
  }

  /**
   * Whether the feature emits annotations on the getters - on and placed there.
   *
   * @return {@code true} when getter annotations should be emitted.
   */
  public boolean emitsOnGetters() {
    return enabled && annotateGetters;
  }

  /**
   * Applies a per-schema {@link JacksonOverride} over this config; unset override flags keep this
   * instance's value.
   *
   * @param override the override.
   * @return the resolved config.
   */
  JacksonConfig withOverride(JacksonOverride override) {
    return new JacksonConfig(
        override.getEnabled().orElse(enabled),
        override.getAnnotateFields().orElse(annotateFields),
        override.getAnnotateGetters().orElse(annotateGetters));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JacksonConfig other)) {
      return false;
    }
    return enabled == other.enabled
        && annotateFields == other.annotateFields
        && annotateGetters == other.annotateGetters;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, annotateFields, annotateGetters);
  }

  @Override
  public String toString() {
    return "JacksonConfig["
        + ("enabled=" + enabled)
        + (", annotateFields=" + annotateFields)
        + (", annotateGetters=" + annotateGetters)
        + "]";
  }

  /** Accumulates a {@link JacksonConfig}; starts at the feature defaults. */
  public static final class Builder {

    private boolean enabled = false;
    private boolean annotateFields = false;
    private boolean annotateGetters = true;

    private Builder() {}

    /**
     * Turns the feature on or off.
     *
     * @param value {@code true} to emit the feature's annotations.
     * @return {@code this}.
     */
    public Builder enabled(boolean value) {
      this.enabled = value;
      return this;
    }

    /**
     * Places the feature's annotations on the generated fields.
     *
     * @param value {@code true} to annotate fields.
     * @return {@code this}.
     */
    public Builder annotateFields(boolean value) {
      this.annotateFields = value;
      return this;
    }

    /**
     * Places the feature's annotations on the generated getters.
     *
     * @param value {@code true} to annotate getters.
     * @return {@code this}.
     */
    public Builder annotateGetters(boolean value) {
      this.annotateGetters = value;
      return this;
    }

    /**
     * Builds the configuration from the accumulated flags.
     *
     * @return a new {@link JacksonConfig}.
     */
    public JacksonConfig build() {
      return new JacksonConfig(enabled, annotateFields, annotateGetters);
    }
  }
}
