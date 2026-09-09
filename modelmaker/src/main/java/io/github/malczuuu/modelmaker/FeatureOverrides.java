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
 * top-level {@code "features"} object. The {@code jackson} / {@code validation} / {@code openApi}
 * overrides are {@link JacksonOverride} / {@link ValidationOverride} / {@link OpenApiOverride}
 * (each flag {@code null} when unset); {@code withers} / {@code preferPrimitives} are {@code null}
 * when unset. Built through {@link #builder()} or {@link #none()}.
 */
public final class FeatureOverrides {

  private static final FeatureOverrides NONE =
      new FeatureOverrides(
          JacksonOverride.none(), ValidationOverride.none(), OpenApiOverride.none(), null, null);

  private final JacksonOverride jackson;
  private final ValidationOverride validation;
  private final OpenApiOverride openApi;
  private final @Nullable Boolean withers;
  private final @Nullable Boolean preferPrimitives;

  private FeatureOverrides(
      JacksonOverride jackson,
      ValidationOverride validation,
      OpenApiOverride openApi,
      @Nullable Boolean withers,
      @Nullable Boolean preferPrimitives) {
    this.jackson = jackson;
    this.validation = validation;
    this.openApi = openApi;
    this.withers = withers;
    this.preferPrimitives = preferPrimitives;
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
  static Builder builder() {
    return new Builder();
  }

  /**
   * The {@code features.jackson} override.
   *
   * @return the override ({@link JacksonOverride#none()} when unset).
   */
  public JacksonOverride getJackson() {
    return jackson;
  }

  /**
   * The {@code features.validation} override.
   *
   * @return the override ({@link ValidationOverride#none()} when unset).
   */
  public ValidationOverride getValidation() {
    return validation;
  }

  /**
   * The {@code features.openApi} override.
   *
   * @return the override ({@link OpenApiOverride#none()} when unset).
   */
  public OpenApiOverride getOpenApi() {
    return openApi;
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
   * Applies every set flag over {@code baseOptions}, leaving unset flags at the base value.
   *
   * @param baseOptions the project's default options.
   * @return a new {@link ModelOptions} with this instance's overrides applied.
   */
  public ModelOptions applyOn(ModelOptions baseOptions) {
    ModelOptions.Builder builder = baseOptions.mutate();
    getPreferPrimitives().ifPresent(builder::preferPrimitives);
    getWithers().ifPresent(builder::withers);
    builder.jackson(baseOptions.getJackson().withOverride(jackson));
    builder.validation(baseOptions.getValidation().withOverride(validation));
    builder.openApi(baseOptions.getOpenApi().withOverride(openApi));
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
    return jackson.equals(other.jackson)
        && validation.equals(other.validation)
        && openApi.equals(other.openApi)
        && Objects.equals(withers, other.withers)
        && Objects.equals(preferPrimitives, other.preferPrimitives);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jackson, validation, openApi, withers, preferPrimitives);
  }

  @Override
  public String toString() {
    return "FeatureOverrides["
        + ("jackson=" + jackson)
        + (", validation=" + validation)
        + (", openApi=" + openApi)
        + (", withers=" + withers)
        + (", preferPrimitives=" + preferPrimitives)
        + "]";
  }

  /** Accumulates {@link FeatureOverrides}; every flag starts unset. */
  public static final class Builder {

    private JacksonOverride jackson = JacksonOverride.none();
    private ValidationOverride validation = ValidationOverride.none();
    private OpenApiOverride openApi = OpenApiOverride.none();
    private @Nullable Boolean withers = null;
    private @Nullable Boolean preferPrimitives = null;

    private Builder() {}

    /**
     * Sets the {@code features.jackson} override.
     *
     * @param value the override.
     * @return {@code this}.
     */
    Builder jackson(JacksonOverride value) {
      this.jackson = value;
      return this;
    }

    /**
     * Sets the {@code features.validation} override.
     *
     * @param value the override.
     * @return {@code this}.
     */
    Builder validation(ValidationOverride value) {
      this.validation = value;
      return this;
    }

    /**
     * Sets the {@code features.openApi} override.
     *
     * @param value the override.
     * @return {@code this}.
     */
    Builder openApi(OpenApiOverride value) {
      this.openApi = value;
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
     * Builds the overrides from the accumulated flags.
     *
     * @return a new {@link FeatureOverrides}.
     */
    public FeatureOverrides build() {
      return new FeatureOverrides(jackson, validation, openApi, withers, preferPrimitives);
    }

    @Override
    public String toString() {
      return "FeatureOverrides.Builder["
          + ("jackson=" + jackson)
          + (", validation=" + validation)
          + (", openApi=" + openApi)
          + (", withers=" + withers)
          + (", preferPrimitives=" + preferPrimitives)
          + "]";
    }
  }
}
