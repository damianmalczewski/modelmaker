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
 * Toggles for what {@link JavaModelMaker} puts on the generated model - the Java-side subset of
 * {@code modelmaker { features { } }}, with any per-schema feature overrides already applied. Built
 * through {@link #builder()}; every flag defaults to {@code false}.
 */
public final class ModelOptions {

  private static final ModelOptions DEFAULT_VALUE = ModelOptions.builder().build();

  private final boolean preferPrimitives;
  private final boolean withers;
  private final boolean jackson;
  private final boolean validation;
  private final boolean openapi;

  private ModelOptions(
      boolean preferPrimitives,
      boolean withers,
      boolean jackson,
      boolean validation,
      boolean openapi) {
    this.preferPrimitives = preferPrimitives;
    this.withers = withers;
    this.jackson = jackson;
    this.validation = validation;
    this.openapi = openapi;
  }

  /**
   * The default options - every flag {@code false}.
   *
   * @return the shared default instance.
   */
  public static ModelOptions defaults() {
    return DEFAULT_VALUE;
  }

  /**
   * Starts accumulating a new options set.
   *
   * @return a new {@link Builder}, every flag starting {@code false}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Always-set scalar fields use {@code int} / {@code double} / {@code boolean} when {@code true}.
   *
   * @return {@code true} when on.
   */
  public boolean isPreferPrimitives() {
    return preferPrimitives;
  }

  /**
   * Whether a {@code withXyz(value)} single-field copy method is emitted per property.
   *
   * @return {@code true} when on.
   */
  public boolean isWithers() {
    return withers;
  }

  /**
   * Whether Jackson annotations are emitted.
   *
   * @return {@code true} when on.
   */
  public boolean isJackson() {
    return jackson;
  }

  /**
   * Whether {@code jakarta.validation} annotations are emitted.
   *
   * @return {@code true} when on.
   */
  public boolean isValidation() {
    return validation;
  }

  /**
   * Whether OpenAPI ({@code io.swagger.v3.oas.annotations}) {@code @Schema} annotations are
   * emitted.
   *
   * @return {@code true} when on.
   */
  public boolean isOpenapi() {
    return openapi;
  }

  /**
   * Starts a new {@link Builder} pre-filled with this instance's flags, for producing a modified
   * copy.
   *
   * @return the pre-filled builder.
   */
  public ModelOptions.Builder mutate() {
    return builder()
        .preferPrimitives(preferPrimitives)
        .withers(withers)
        .jackson(jackson)
        .validation(validation)
        .openapi(openapi);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ModelOptions other)) {
      return false;
    }
    return preferPrimitives == other.preferPrimitives
        && withers == other.withers
        && jackson == other.jackson
        && validation == other.validation
        && openapi == other.openapi;
  }

  @Override
  public int hashCode() {
    return Objects.hash(preferPrimitives, withers, jackson, validation, openapi);
  }

  @Override
  public String toString() {
    return "ModelOptions["
        + ("preferPrimitives=" + preferPrimitives)
        + (", withers=" + withers)
        + (", jackson=" + jackson)
        + (", validation=" + validation)
        + (", openapi=" + openapi)
        + "]";
  }

  /** Accumulates {@link ModelOptions}; every flag starts {@code false}. */
  public static final class Builder {

    private boolean preferPrimitives = false;
    private boolean withers = false;
    private boolean jackson = false;
    private boolean validation = false;
    private boolean openapi = false;

    private Builder() {}

    /**
     * Always-set scalar fields use {@code int} / {@code double} / {@code boolean} when {@code
     * true}.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder preferPrimitives(boolean value) {
      this.preferPrimitives = value;
      return this;
    }

    /**
     * Emit a {@code withXyz(value)} single-field copy method per property when {@code true}.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder withers(boolean value) {
      this.withers = value;
      return this;
    }

    /**
     * Emit Jackson annotations when {@code true}.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder jackson(boolean value) {
      this.jackson = value;
      return this;
    }

    /**
     * Emit {@code jakarta.validation} annotations when {@code true}.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder validation(boolean value) {
      this.validation = value;
      return this;
    }

    /**
     * Emit OpenAPI ({@code io.swagger.v3.oas.annotations}) {@code @Schema} annotations when {@code
     * true}.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder openapi(boolean value) {
      this.openapi = value;
      return this;
    }

    /**
     * Builds the options from the accumulated flags.
     *
     * @return a new {@link ModelOptions}.
     */
    public ModelOptions build() {
      return new ModelOptions(preferPrimitives, withers, jackson, validation, openapi);
    }

    @Override
    public String toString() {
      return "ModelOptions.Builder["
          + ("preferPrimitives=" + preferPrimitives)
          + (", withers=" + withers)
          + (", jackson=" + jackson)
          + (", validation=" + validation)
          + (", openapi=" + openapi)
          + "]";
    }
  }
}
