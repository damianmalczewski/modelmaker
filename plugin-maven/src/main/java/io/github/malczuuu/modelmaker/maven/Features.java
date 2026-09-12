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

package io.github.malczuuu.modelmaker.maven;

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
  private AnnotationFeature validation = new AnnotationFeature();
  private AnnotationFeature openApi = new AnnotationFeature();
  private ToggleFeature withers = new ToggleFeature();
  private ToggleFeature preferPrimitives = new ToggleFeature();

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
  public AnnotationFeature getValidation() {
    return validation;
  }

  /**
   * Sets the {@code validation} feature.
   *
   * @param validation the feature.
   */
  public void setValidation(AnnotationFeature validation) {
    this.validation = validation;
  }

  /**
   * The {@code openApi} feature.
   *
   * @return the feature.
   */
  public AnnotationFeature getOpenApi() {
    return openApi;
  }

  /**
   * Sets the {@code openApi} feature.
   *
   * @param openApi the feature.
   */
  public void setOpenApi(AnnotationFeature openApi) {
    this.openApi = openApi;
  }

  /**
   * The {@code withers} feature.
   *
   * @return the feature.
   */
  public ToggleFeature getWithers() {
    return withers;
  }

  /**
   * Sets the {@code withers} feature.
   *
   * @param withers the feature.
   */
  public void setWithers(ToggleFeature withers) {
    this.withers = withers;
  }

  /**
   * The {@code preferPrimitives} feature.
   *
   * @return the feature.
   */
  public ToggleFeature getPreferPrimitives() {
    return preferPrimitives;
  }

  /**
   * Sets the {@code preferPrimitives} feature.
   *
   * @param preferPrimitives the feature.
   */
  public void setPreferPrimitives(ToggleFeature preferPrimitives) {
    this.preferPrimitives = preferPrimitives;
  }
}
