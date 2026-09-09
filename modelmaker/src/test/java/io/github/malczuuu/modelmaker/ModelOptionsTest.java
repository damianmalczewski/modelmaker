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

  private static ModelOptions options(
      boolean preferPrimitives, boolean withers, boolean jackson, boolean validation) {
    return options(preferPrimitives, withers, jackson, validation, false);
  }

  private static ModelOptions options(
      boolean preferPrimitives,
      boolean withers,
      boolean jackson,
      boolean validation,
      boolean openapi) {
    return ModelOptions.builder()
        .preferPrimitives(preferPrimitives)
        .withers(withers)
        .jackson(jackson)
        .validation(validation)
        .openapi(openapi)
        .build();
  }

  @Test
  void equalWhenEveryFlagMatches() {
    assertThat(options(true, false, true, false))
        .isEqualTo(options(true, false, true, false))
        .hasSameHashCodeAs(options(true, false, true, false));
  }

  @Test
  void notEqualWhenAnyFlagDiffers() {
    ModelOptions base = options(true, false, true, false);

    assertThat(base)
        .isNotEqualTo(options(false, false, true, false))
        .isNotEqualTo(options(true, true, true, false))
        .isNotEqualTo(options(true, false, false, false))
        .isNotEqualTo(options(true, false, true, true))
        .isNotEqualTo(options(true, false, true, false, true));
  }

  @Test
  void mutateCarriesOpenapi() {
    assertThat(options(false, false, false, false, true).mutate().build().isOpenapi()).isTrue();
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(options(true, false, true, false)).isNotEqualTo(null).isNotEqualTo("ModelOptions");
  }

  @Test
  void toStringReportsEveryFlag() {
    assertThat(options(true, false, true, false).toString())
        .isEqualTo(
            "ModelOptions[preferPrimitives=true, withers=false, jackson=true, validation=false,"
                + " openapi=false]");
  }
}
