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

package io.github.malczuuu.modelmaker.maven.config;

import io.github.malczuuu.modelmaker.JacksonConfig;
import io.github.malczuuu.modelmaker.ModelOptions;
import io.github.malczuuu.modelmaker.OpenApiConfig;
import io.github.malczuuu.modelmaker.ValidationConfig;

/**
 * The {@code <features>} block of the plugin configuration - the Maven face of {@code modelmaker {
 * features { } }}. Every feature is off by default, and each is overridable per schema file through
 * that file's own {@code "features"} object.
 */
public class Features {

  /** Creates a new {@link Features} with every feature turned off. */
  public Features() {}

  private JacksonFeature jackson = new JacksonFeature();
  private ValidationFeature validation = new ValidationFeature();
  private OpenApiFeature openApi = new OpenApiFeature();
  private WithersFeature withers = new WithersFeature();
  private PreferPrimitivesFeature preferPrimitives = new PreferPrimitivesFeature();

  /**
   * Turns these features into the options the generator takes.
   *
   * @return the resolved options.
   */
  public ModelOptions toModelOptions() {
    return ModelOptions.builder()
        .jackson(
            JacksonConfig.builder()
                .enabled(jackson.isEnabled())
                .annotateFields(jackson.isAnnotateFields())
                .annotateGetters(jackson.isAnnotateGetters())
                .includeNonNull(jackson.isIncludeNonNull())
                .build())
        .validation(
            ValidationConfig.builder()
                .enabled(validation.isEnabled())
                .annotateFields(validation.isAnnotateFields())
                .annotateGetters(validation.isAnnotateGetters())
                .build())
        .openApi(
            OpenApiConfig.builder()
                .enabled(openApi.isEnabled())
                .annotateFields(openApi.isAnnotateFields())
                .annotateGetters(openApi.isAnnotateGetters())
                .build())
        .withers(withers.isEnabled())
        .preferPrimitives(preferPrimitives.isEnabled())
        .build();
  }

  /**
   * The {@code jackson} feature.
   *
   * @return the feature.
   */
  public JacksonFeature getJackson() {
    return jackson;
  }

  /**
   * Sets the {@code jackson} feature.
   *
   * @param jackson the feature.
   */
  public void setJackson(JacksonFeature jackson) {
    this.jackson = jackson;
  }

  /**
   * The {@code validation} feature.
   *
   * @return the feature.
   */
  public ValidationFeature getValidation() {
    return validation;
  }

  /**
   * Sets the {@code validation} feature.
   *
   * @param validation the feature.
   */
  public void setValidation(ValidationFeature validation) {
    this.validation = validation;
  }

  /**
   * The {@code openApi} feature.
   *
   * @return the feature.
   */
  public OpenApiFeature getOpenApi() {
    return openApi;
  }

  /**
   * Sets the {@code openApi} feature.
   *
   * @param openApi the feature.
   */
  public void setOpenApi(OpenApiFeature openApi) {
    this.openApi = openApi;
  }

  /**
   * The {@code withers} feature.
   *
   * @return the feature.
   */
  public WithersFeature getWithers() {
    return withers;
  }

  /**
   * Sets the {@code withers} feature.
   *
   * @param withers the feature.
   */
  public void setWithers(WithersFeature withers) {
    this.withers = withers;
  }

  /**
   * The {@code preferPrimitives} feature.
   *
   * @return the feature.
   */
  public PreferPrimitivesFeature getPreferPrimitives() {
    return preferPrimitives;
  }

  /**
   * Sets the {@code preferPrimitives} feature.
   *
   * @param preferPrimitives the feature.
   */
  public void setPreferPrimitives(PreferPrimitivesFeature preferPrimitives) {
    this.preferPrimitives = preferPrimitives;
  }
}
