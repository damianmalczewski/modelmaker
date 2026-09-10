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

/**
 * {@link JacksonConfig}, {@link ValidationConfig} and {@link OpenApiConfig} share their whole
 * shape; exercised here through {@link JacksonConfig}, plus the cross-type checks that matter.
 */
class AnnotationConfigTest {

  @Test
  void defaultsAreDisabledWithGetterPlacement() {
    JacksonConfig c = JacksonConfig.defaults();

    assertThat(c.isEnabled()).isFalse();
    assertThat(c.isAnnotateFields()).isFalse();
    assertThat(c.isAnnotateGetters()).isTrue();
    assertThat(c.emitsOnFields()).isFalse();
    assertThat(c.emitsOnGetters()).isFalse();
  }

  @Test
  void emitsOnlyWhenEnabledAndPlacedAtThatSite() {
    JacksonConfig fieldsOnly =
        JacksonConfig.builder().enabled(true).annotateFields(true).annotateGetters(false).build();
    assertThat(fieldsOnly.emitsOnFields()).isTrue();
    assertThat(fieldsOnly.emitsOnGetters()).isFalse();

    JacksonConfig both =
        JacksonConfig.builder().enabled(true).annotateFields(true).annotateGetters(true).build();
    assertThat(both.emitsOnFields()).isTrue();
    assertThat(both.emitsOnGetters()).isTrue();

    JacksonConfig disabled =
        JacksonConfig.builder().enabled(false).annotateFields(true).annotateGetters(true).build();
    assertThat(disabled.emitsOnFields()).isFalse();
    assertThat(disabled.emitsOnGetters()).isFalse();
  }

  @Test
  void withOverrideAppliesOnlySetFlags() {
    JacksonConfig base =
        JacksonConfig.builder().enabled(true).annotateFields(false).annotateGetters(true).build();

    JacksonConfig result = base.withOverride(new JacksonOverride(null, true, null));

    assertThat(result.isEnabled()).isTrue(); // kept
    assertThat(result.isAnnotateFields()).isTrue(); // overridden
    assertThat(result.isAnnotateGetters()).isTrue(); // kept
    assertThat(base.withOverride(JacksonOverride.none())).isEqualTo(base);
  }

  @Test
  void equalityAndToStringAreTypeSpecific() {
    assertThat(
            JacksonConfig.builder()
                .enabled(true)
                .annotateFields(false)
                .annotateGetters(true)
                .build())
        .isEqualTo(
            JacksonConfig.builder()
                .enabled(true)
                .annotateFields(false)
                .annotateGetters(true)
                .build())
        .hasSameHashCodeAs(
            JacksonConfig.builder()
                .enabled(true)
                .annotateFields(false)
                .annotateGetters(true)
                .build())
        .isNotEqualTo(
            JacksonConfig.builder()
                .enabled(false)
                .annotateFields(false)
                .annotateGetters(true)
                .build())
        .isNotEqualTo(null)
        .isNotEqualTo("JacksonConfig");

    assertThat(JacksonConfig.defaults().toString())
        .isEqualTo("JacksonConfig[enabled=false, annotateFields=false, annotateGetters=true]");
    assertThat(OpenApiConfig.defaults().toString())
        .isEqualTo("OpenApiConfig[enabled=false, annotateFields=false, annotateGetters=true]");
  }
}
