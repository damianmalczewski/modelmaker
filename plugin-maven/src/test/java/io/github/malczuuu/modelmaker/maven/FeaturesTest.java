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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.malczuuu.modelmaker.ModelOptions;
import org.junit.jupiter.api.Test;

class FeaturesTest {

  @Test
  void everyFeatureIsOffByDefault() {
    assertThat(new Features().toModelOptions()).isEqualTo(ModelOptions.defaults());
  }

  @Test
  void enabledFeaturesAnnotateGettersByDefault() {
    Features features = new Features();
    features.getJackson().setEnabled(true);
    features.getValidation().setEnabled(true);
    features.getOpenApi().setEnabled(true);

    ModelOptions options = features.toModelOptions();

    assertThat(options.getJackson().emitsOnGetters()).isTrue();
    assertThat(options.getJackson().emitsOnFields()).isFalse();
    assertThat(options.getValidation().emitsOnGetters()).isTrue();
    assertThat(options.getOpenApi().emitsOnGetters()).isTrue();
    assertThat(options.getJackson().emitsIncludeNonNull()).isFalse();
  }

  @Test
  void placementAndTogglesAreCarriedOver() {
    Features features = new Features();
    features.getJackson().setEnabled(true);
    features.getJackson().setIncludeNonNull(true);
    features.getValidation().setEnabled(true);
    features.getValidation().setAnnotateFields(true);
    features.getValidation().setAnnotateGetters(false);
    features.getWithers().setEnabled(true);
    features.getPreferPrimitives().setEnabled(true);

    ModelOptions options = features.toModelOptions();

    assertThat(options.getJackson().emitsIncludeNonNull()).isTrue();
    assertThat(options.getValidation().emitsOnFields()).isTrue();
    assertThat(options.getValidation().emitsOnGetters()).isFalse();
    assertThat(options.isWithers()).isTrue();
    assertThat(options.isPreferPrimitives()).isTrue();
  }
}
