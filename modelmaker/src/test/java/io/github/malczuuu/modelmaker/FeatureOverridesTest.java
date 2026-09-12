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

  /** primitives off, withers on, jackson on, validation off, openApi off. */
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

    assertThat(result).isEqualTo(base);
  }

  @Test
  void aSetEnabledOverrideReplacesOnlyThatFeature() {
    ModelOptions result =
        FeatureOverrides.builder().jackson(JacksonOverride.enabled(false)).build().applyOn(base());

    assertThat(result.getJackson().isEnabled()).isFalse(); // overridden
    assertThat(result.getValidation().isEnabled()).isFalse(); // untouched
    assertThat(result.isWithers()).isTrue(); // untouched
    assertThat(result.isPreferPrimitives()).isFalse(); // untouched
  }

  @Test
  void aPlacementOverrideLeavesEnabledAtTheBase() {
    ModelOptions result =
        FeatureOverrides.builder()
            .jackson(new JacksonOverride(null, true, null))
            .build()
            .applyOn(base());

    assertThat(result.getJackson().isEnabled()).isTrue(); // base
    assertThat(result.getJackson().isAnnotateFields()).isTrue(); // overridden
    assertThat(result.getJackson().isAnnotateGetters()).isTrue(); // base default
  }

  @Test
  void openApiOverrideIsApplied() {
    assertThat(
            FeatureOverrides.builder()
                .openApi(OpenApiOverride.enabled(true))
                .build()
                .applyOn(base())
                .getOpenApi()
                .isEnabled())
        .isTrue();
    assertThat(FeatureOverrides.none().applyOn(base()).getOpenApi().isEnabled()).isFalse();
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
            .jackson(JacksonOverride.enabled(false))
            .validation(ValidationOverride.enabled(true))
            .build()
            .applyOn(base());

    assertThat(result.isPreferPrimitives()).isTrue();
    assertThat(result.isWithers()).isFalse();
    assertThat(result.getJackson().isEnabled()).isFalse();
    assertThat(result.getValidation().isEnabled()).isTrue();
  }

  @Test
  void partialOverrideMixesOverrideAndBase() {
    ModelOptions result =
        FeatureOverrides.builder().withers(false).preferPrimitives(true).build().applyOn(base());

    assertThat(result.isWithers()).isFalse(); // override
    assertThat(result.isPreferPrimitives()).isTrue(); // override
    assertThat(result.getJackson().isEnabled()).isTrue(); // base
    assertThat(result.getValidation().isEnabled()).isFalse(); // base
  }

  @Test
  void aNoneOverrideIsTreatedAsUnset() {
    ModelOptions result =
        FeatureOverrides.builder().jackson(JacksonOverride.none()).build().applyOn(base());

    assertThat(result.getJackson()).isEqualTo(base().getJackson());
  }

  @Test
  void applyOnDoesNotMutateTheBase() {
    ModelOptions base = base();

    FeatureOverrides.builder()
        .jackson(JacksonOverride.enabled(false))
        .validation(ValidationOverride.enabled(true))
        .build()
        .applyOn(base);

    assertThat(base.getJackson().isEnabled()).isTrue();
    assertThat(base.getValidation().isEnabled()).isFalse();
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
    FeatureOverrides a = FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build();
    FeatureOverrides b = FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void notEqualWhenAFlagDiffers() {
    FeatureOverrides a = FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build();
    FeatureOverrides b = FeatureOverrides.builder().jackson(JacksonOverride.enabled(false)).build();

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(FeatureOverrides.none()).isNotEqualTo(null).isNotEqualTo("none");
  }

  @Test
  void toStringReportsEveryFlag() {
    assertThat(FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build().toString())
        .isEqualTo(
            "FeatureOverrides["
                + "jackson=JacksonOverride[enabled=true, annotateFields=null,"
                + " annotateGetters=null, includeNonNull=null], "
                + "validation=ValidationOverride[enabled=null, annotateFields=null,"
                + " annotateGetters=null], "
                + "openApi=OpenApiOverride[enabled=null, annotateFields=null,"
                + " annotateGetters=null], "
                + "withers=null, preferPrimitives=null]");
  }
}
