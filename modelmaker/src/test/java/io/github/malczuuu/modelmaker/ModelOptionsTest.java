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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelOptionsTest {

  @Test
  void defaultsHaveEveryFeatureOff() {
    ModelOptions o = ModelOptions.defaults();

    assertThat(o.getJackson().isEnabled()).isFalse();
    assertThat(o.getValidation().isEnabled()).isFalse();
    assertThat(o.getOpenApi().isEnabled()).isFalse();
    assertThat(o.isWithers()).isFalse();
    assertThat(o.isPreferPrimitives()).isFalse();
  }

  @Test
  void booleanShorthandEnablesTheFeatureKeepingGetterPlacement() {
    ModelOptions o = ModelOptions.builder().validation(true).build();

    assertThat(o.getValidation().isEnabled()).isTrue();
    assertThat(o.getValidation().emitsOnGetters()).isTrue();
    assertThat(o.getValidation().emitsOnFields()).isFalse();
  }

  @Test
  void anExplicitConfigIsKept() {
    OpenApiConfig cfg =
        OpenApiConfig.builder().enabled(true).annotateFields(true).annotateGetters(false).build();

    ModelOptions o = ModelOptions.builder().openApi(cfg).build();

    assertThat(o.getOpenApi()).isEqualTo(cfg);
    assertThat(o.getOpenApi().emitsOnFields()).isTrue();
    assertThat(o.getOpenApi().emitsOnGetters()).isFalse();
  }

  @Test
  void mutateRoundTrips() {
    ModelOptions o =
        ModelOptions.builder()
            .preferPrimitives(true)
            .withers(true)
            .jackson(true)
            .validation(
                ValidationConfig.builder()
                    .enabled(true)
                    .annotateFields(true)
                    .annotateGetters(true)
                    .build())
            .build();

    assertThat(o.mutate().build()).isEqualTo(o);
  }

  @Test
  void equalWhenEveryValueMatches() {
    assertThat(ModelOptions.builder().jackson(true).build())
        .isEqualTo(ModelOptions.builder().jackson(true).build())
        .hasSameHashCodeAs(ModelOptions.builder().jackson(true).build());
  }

  @Test
  void notEqualWhenAnyValueDiffers() {
    ModelOptions base = ModelOptions.builder().jackson(true).build();

    assertThat(base)
        .isNotEqualTo(ModelOptions.builder().jackson(false).build())
        .isNotEqualTo(ModelOptions.builder().jackson(true).withers(true).build())
        .isNotEqualTo(
            ModelOptions.builder()
                .jackson(
                    JacksonConfig.builder()
                        .enabled(true)
                        .annotateFields(true)
                        .annotateGetters(true)
                        .build())
                .build());
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(ModelOptions.defaults()).isNotEqualTo(null).isNotEqualTo("ModelOptions");
  }

  @Test
  void toStringReportsEveryValue() {
    assertThat(ModelOptions.builder().preferPrimitives(true).jackson(true).build().toString())
        .isEqualTo(
            "ModelOptions[preferPrimitives=true, withers=false, "
                + "jackson=JacksonConfig[enabled=true, annotateFields=false,"
                + " annotateGetters=true], "
                + "validation=ValidationConfig[enabled=false, annotateFields=false,"
                + " annotateGetters=true], "
                + "openApi=OpenApiConfig[enabled=false, annotateFields=false,"
                + " annotateGetters=true]]");
  }
}
