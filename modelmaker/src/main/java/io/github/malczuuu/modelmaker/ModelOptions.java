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
 * through {@link #builder()}.
 *
 * <p>The three annotation-emitting features ({@code jackson}, {@code validation}, {@code openApi})
 * each carry their own config ({@link JacksonConfig}, {@link ValidationConfig}, {@link
 * OpenApiConfig}) - on/off plus field / getter placement. {@code withers} and {@code
 * preferPrimitives} are plain toggles.
 */
public final class ModelOptions {

  private static final ModelOptions DEFAULT_VALUE = ModelOptions.builder().build();

  private final boolean preferPrimitives;
  private final boolean withers;
  private final JacksonConfig jackson;
  private final ValidationConfig validation;
  private final OpenApiConfig openApi;

  private ModelOptions(
      boolean preferPrimitives,
      boolean withers,
      JacksonConfig jackson,
      ValidationConfig validation,
      OpenApiConfig openApi) {
    this.preferPrimitives = preferPrimitives;
    this.withers = withers;
    this.jackson = jackson;
    this.validation = validation;
    this.openApi = openApi;
  }

  /**
   * The default options - every feature off, annotations placed on getters when later enabled.
   *
   * @return the shared default instance.
   */
  public static ModelOptions defaults() {
    return DEFAULT_VALUE;
  }

  /**
   * Starts accumulating a new options set.
   *
   * @return a new {@link Builder}.
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
   * Jackson annotation configuration - on/off plus field / getter placement.
   *
   * @return the configuration.
   */
  public JacksonConfig getJackson() {
    return jackson;
  }

  /**
   * {@code jakarta.validation} annotation configuration - on/off plus field / getter placement.
   *
   * @return the configuration.
   */
  public ValidationConfig getValidation() {
    return validation;
  }

  /**
   * OpenAPI ({@code io.swagger.v3.oas.annotations}) {@code @Schema} configuration - on/off plus
   * field / getter placement.
   *
   * @return the configuration.
   */
  public OpenApiConfig getOpenApi() {
    return openApi;
  }

  /**
   * Starts a new {@link Builder} pre-filled with this instance's values, for producing a modified
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
        .openApi(openApi);
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
        && jackson.equals(other.jackson)
        && validation.equals(other.validation)
        && openApi.equals(other.openApi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(preferPrimitives, withers, jackson, validation, openApi);
  }

  @Override
  public String toString() {
    return "ModelOptions["
        + ("preferPrimitives=" + preferPrimitives)
        + (", withers=" + withers)
        + (", jackson=" + jackson)
        + (", validation=" + validation)
        + (", openApi=" + openApi)
        + "]";
  }

  /** Accumulates {@link ModelOptions}. */
  public static final class Builder {

    private boolean preferPrimitives = false;
    private boolean withers = false;
    private JacksonConfig jackson = JacksonConfig.defaults();
    private ValidationConfig validation = ValidationConfig.defaults();
    private OpenApiConfig openApi = OpenApiConfig.defaults();

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
     * Turns Jackson annotations on or off, keeping the default getter placement.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder jackson(boolean value) {
      this.jackson = JacksonConfig.builder().enabled(value).build();
      return this;
    }

    /**
     * Sets the jackson configuration, e.g. from {@link JacksonConfig#builder()}.
     *
     * @param value the configuration.
     * @return {@code this}.
     */
    public Builder jackson(JacksonConfig value) {
      this.jackson = value;
      return this;
    }

    /**
     * Turns {@code jakarta.validation} annotations on or off, keeping the default getter placement.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder validation(boolean value) {
      this.validation = ValidationConfig.builder().enabled(value).build();
      return this;
    }

    /**
     * Sets the validation configuration, e.g. from {@link ValidationConfig#builder()}.
     *
     * @param value the configuration.
     * @return {@code this}.
     */
    public Builder validation(ValidationConfig value) {
      this.validation = value;
      return this;
    }

    /**
     * Turns OpenAPI {@code @Schema} annotations on or off, keeping the default getter placement.
     *
     * @param value {@code true} to turn it on.
     * @return {@code this}.
     */
    public Builder openApi(boolean value) {
      this.openApi = OpenApiConfig.builder().enabled(value).build();
      return this;
    }

    /**
     * Sets the openApi configuration, e.g. from {@link OpenApiConfig#builder()}.
     *
     * @param value the configuration.
     * @return {@code this}.
     */
    public Builder openApi(OpenApiConfig value) {
      this.openApi = value;
      return this;
    }

    /**
     * Builds the options from the accumulated values.
     *
     * @return a new {@link ModelOptions}.
     */
    public ModelOptions build() {
      return new ModelOptions(preferPrimitives, withers, jackson, validation, openApi);
    }

    @Override
    public String toString() {
      return "ModelOptions.Builder["
          + ("preferPrimitives=" + preferPrimitives)
          + (", withers=" + withers)
          + (", jackson=" + jackson)
          + (", validation=" + validation)
          + (", openApi=" + openApi)
          + "]";
    }
  }
}
