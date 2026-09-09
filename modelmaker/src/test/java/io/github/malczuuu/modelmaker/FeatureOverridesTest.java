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

class FeatureOverridesTest {

  /** primitives off, withers on, jackson on, validation off. */
  private static ModelOptions base() {
    return ModelOptions.builder()
        .preferPrimitives(false)
        .withers(true)
        .jackson(true)
        .validation(false)
        .build();
  }

  @Test
  void noneLeavesEveryFlagUntouched() {
    ModelOptions base = base();

    ModelOptions result = FeatureOverrides.none().applyOn(base);

    assertThat(result.isPreferPrimitives()).isEqualTo(base.isPreferPrimitives());
    assertThat(result.isWithers()).isEqualTo(base.isWithers());
    assertThat(result.isJackson()).isEqualTo(base.isJackson());
    assertThat(result.isValidation()).isEqualTo(base.isValidation());
  }

  @Test
  void aSetOverrideReplacesOnlyThatFlag() {
    ModelOptions result = FeatureOverrides.builder().jackson(false).build().applyOn(base());

    assertThat(result.isJackson()).isFalse(); // overridden
    assertThat(result.isValidation()).isFalse(); // untouched
    assertThat(result.isWithers()).isTrue(); // untouched
    assertThat(result.isPreferPrimitives()).isFalse(); // untouched
  }

  @Test
  void openapiOverrideIsApplied() {
    assertThat(FeatureOverrides.builder().openapi(true).build().applyOn(base()).isOpenapi())
        .isTrue();
    assertThat(FeatureOverrides.none().applyOn(base()).isOpenapi()).isFalse();
  }

  @Test
  void anExplicitTrueOverridesAFalseBase() {
    ModelOptions result = FeatureOverrides.builder().preferPrimitives(true).build().applyOn(base());

    assertThat(result.isPreferPrimitives()).isTrue();
    assertThat(result.isWithers()).isTrue();
  }

  @Test
  void everyFlagOverriddenWinsRegardlessOfBase() {
    ModelOptions result =
        FeatureOverrides.builder()
            .preferPrimitives(true)
            .withers(false)
            .jackson(false)
            .validation(true)
            .build()
            .applyOn(base());

    assertThat(result.isPreferPrimitives()).isTrue();
    assertThat(result.isWithers()).isFalse();
    assertThat(result.isJackson()).isFalse();
    assertThat(result.isValidation()).isTrue();
  }

  @Test
  void partialOverrideMixesOverrideAndBase() {
    ModelOptions result =
        FeatureOverrides.builder().withers(false).preferPrimitives(true).build().applyOn(base());

    assertThat(result.isWithers()).isFalse(); // override
    assertThat(result.isPreferPrimitives()).isTrue(); // override
    assertThat(result.isJackson()).isTrue(); // base
    assertThat(result.isValidation()).isFalse(); // base
  }

  @Test
  void aNullOverrideIsTreatedAsUnset() {
    ModelOptions result = FeatureOverrides.builder().jackson(null).build().applyOn(base());

    assertThat(result.isJackson()).isEqualTo(base().isJackson());
  }

  @Test
  void applyOnDoesNotMutateTheBase() {
    ModelOptions base = base();

    FeatureOverrides.builder().jackson(false).validation(true).build().applyOn(base);

    assertThat(base.isJackson()).isTrue();
    assertThat(base.isValidation()).isFalse();
  }

  @Test
  void applyOnReturnsANewInstance() {
    ModelOptions base = base();

    assertThat(FeatureOverrides.none().applyOn(base)).isNotSameAs(base);
  }

  @Test
  void noneEqualsAnEmptyBuiltInstance() {
    assertThat(FeatureOverrides.none())
        .isEqualTo(FeatureOverrides.builder().build())
        .hasSameHashCodeAs(FeatureOverrides.builder().build());
  }

  @Test
  void equalWhenEveryFlagMatches() {
    FeatureOverrides a = FeatureOverrides.builder().jackson(true).validation(false).build();
    FeatureOverrides b = FeatureOverrides.builder().jackson(true).validation(false).build();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void notEqualWhenAFlagDiffers() {
    FeatureOverrides a = FeatureOverrides.builder().jackson(true).build();
    FeatureOverrides b = FeatureOverrides.builder().jackson(false).build();

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(FeatureOverrides.none()).isNotEqualTo(null).isNotEqualTo("none");
  }

  @Test
  void toStringReportsEveryFlag() {
    assertThat(FeatureOverrides.builder().jackson(true).build().toString())
        .isEqualTo(
            "FeatureOverrides[jackson=true, validation=null, withers=null,"
                + " preferPrimitives=null, openapi=null]");
  }
}
