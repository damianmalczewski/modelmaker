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

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTypeTest {

  private static ModelType full() {
    return new ModelType(
        "Widget",
        "com.example.dto",
        "A widget.",
        List.of(
            new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null)),
        List.of(
            new ModelType(
                "Nested", "com.example.dto", null, List.of(), List.of(), FeatureOverrides.none())),
        FeatureOverrides.builder().jackson(true).build());
  }

  @Test
  void equalWhenEveryFieldMatches() {
    assertThat(full()).isEqualTo(full()).hasSameHashCodeAs(full());
  }

  @Test
  void notEqualWhenNameDiffers() {
    ModelType other =
        new ModelType(
            "Other",
            "com.example.dto",
            "A widget.",
            full().getProperties(),
            full().getNested(),
            FeatureOverrides.builder().jackson(true).build());

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualWhenDescriptionDiffers() {
    ModelType other =
        new ModelType(
            "Widget",
            "com.example.dto",
            "A different widget.",
            full().getProperties(),
            full().getNested(),
            FeatureOverrides.builder().jackson(true).build());

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualWhenFeatureOverridesDiffer() {
    assertThat(full()).isNotEqualTo(full().withFeatureOverrides(FeatureOverrides.none()));
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(full()).isNotEqualTo(null).isNotEqualTo("Widget");
  }

  @Test
  void toStringReportsEveryField() {
    assertThat(full().toString())
        .contains("ModelType[")
        .contains("name=Widget")
        .contains("packageName=com.example.dto")
        .contains("description=A widget.")
        .contains("featureOverrides=" + FeatureOverrides.builder().jackson(true).build());
  }
}
