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
 * Per-schema overrides of {@code modelmaker { features { } }}, parsed from a schema file's
 * top-level {@code "features"} object. Each flag is {@code null} when the schema does not set it -
 * the project default then applies. Built through {@link #builder()} or {@link #none()}.
 */
public final class FeatureOverrides {

  private static final FeatureOverrides NONE = new FeatureOverrides(null, null, null, null, null);

  private final @Nullable Boolean jackson;
  private final @Nullable Boolean validation;
  private final @Nullable Boolean withers;
  private final @Nullable Boolean preferPrimitives;
  private final @Nullable Boolean openapi;

  private FeatureOverrides(
      @Nullable Boolean jackson,
      @Nullable Boolean validation,
      @Nullable Boolean withers,
      @Nullable Boolean preferPrimitives,
      @Nullable Boolean openapi) {
    this.jackson = jackson;
    this.validation = validation;
    this.withers = withers;
    this.preferPrimitives = preferPrimitives;
    this.openapi = openapi;
  }

  /**
   * No overrides - every flag falls through to the project default.
   *
   * @return the shared empty instance.
   */
  public static FeatureOverrides none() {
    return NONE;
  }

  /**
   * Starts accumulating a new overrides set.
   *
   * @return a new {@link Builder}, every flag starting unset.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The {@code features.jackson} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getJackson() {
    return Optional.ofNullable(jackson);
  }

  /**
   * The {@code features.validation} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getValidation() {
    return Optional.ofNullable(validation);
  }

  /**
   * The {@code features.withers} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getWithers() {
    return Optional.ofNullable(withers);
  }

  /**
   * The {@code features.preferPrimitives} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getPreferPrimitives() {
    return Optional.ofNullable(preferPrimitives);
  }

  /**
   * The {@code features.openapi} override.
   *
   * @return the override, or empty when unset.
   */
  public Optional<Boolean> getOpenapi() {
    return Optional.ofNullable(openapi);
  }

  /**
   * Applies every set flag over {@code baseOptions}, leaving unset flags at the base value.
   *
   * @param baseOptions the project's default options.
   * @return a new {@link ModelOptions} with this instance's overrides applied.
   */
  public ModelOptions applyOn(ModelOptions baseOptions) {
    ModelOptions.Builder builder = baseOptions.mutate();
    getPreferPrimitives().ifPresent(builder::preferPrimitives);
    getWithers().ifPresent(builder::withers);
    getJackson().ifPresent(builder::jackson);
    getValidation().ifPresent(builder::validation);
    getOpenapi().ifPresent(builder::openapi);
    return builder.build();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FeatureOverrides other)) {
      return false;
    }
    return Objects.equals(jackson, other.jackson)
        && Objects.equals(validation, other.validation)
        && Objects.equals(withers, other.withers)
        && Objects.equals(preferPrimitives, other.preferPrimitives)
        && Objects.equals(openapi, other.openapi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jackson, validation, withers, preferPrimitives, openapi);
  }

  @Override
  public String toString() {
    return "FeatureOverrides["
        + ("jackson=" + jackson)
        + (", validation=" + validation)
        + (", withers=" + withers)
        + (", preferPrimitives=" + preferPrimitives)
        + (", openapi=" + openapi)
        + "]";
  }

  /** Accumulates {@link FeatureOverrides}; every flag starts unset ({@code null}). */
  public static final class Builder {

    private @Nullable Boolean jackson = null;
    private @Nullable Boolean validation = null;
    private @Nullable Boolean withers = null;
    private @Nullable Boolean preferPrimitives = null;
    private @Nullable Boolean openapi = null;

    private Builder() {}

    /**
     * Sets the {@code features.jackson} override.
     *
     * @param value the override, or {@code null} to leave it unset.
     * @return {@code this}.
     */
    public Builder jackson(@Nullable Boolean value) {
      this.jackson = value;
      return this;
    }

    /**
     * Sets the {@code features.validation} override.
     *
     * @param value the override, or {@code null} to leave it unset.
     * @return {@code this}.
     */
    public Builder validation(@Nullable Boolean value) {
      this.validation = value;
      return this;
    }

    /**
     * Sets the {@code features.withers} override.
     *
     * @param value the override, or {@code null} to leave it unset.
     * @return {@code this}.
     */
    public Builder withers(@Nullable Boolean value) {
      this.withers = value;
      return this;
    }

    /**
     * Sets the {@code features.preferPrimitives} override.
     *
     * @param value the override, or {@code null} to leave it unset.
     * @return {@code this}.
     */
    public Builder preferPrimitives(@Nullable Boolean value) {
      this.preferPrimitives = value;
      return this;
    }

    /**
     * Sets the {@code features.openapi} override.
     *
     * @param value the override, or {@code null} to leave it unset.
     * @return {@code this}.
     */
    public Builder openapi(@Nullable Boolean value) {
      this.openapi = value;
      return this;
    }

    /**
     * Builds the overrides from the accumulated flags.
     *
     * @return a new {@link FeatureOverrides}.
     */
    public FeatureOverrides build() {
      return new FeatureOverrides(jackson, validation, withers, preferPrimitives, openapi);
    }

    @Override
    public String toString() {
      return "FeatureOverrides.Builder["
          + ("jackson=" + jackson)
          + (", validation=" + validation)
          + (", withers=" + withers)
          + (", preferPrimitives=" + preferPrimitives)
          + (", openapi=" + openapi)
          + "]";
    }
  }
}
