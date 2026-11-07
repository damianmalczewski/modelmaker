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

import static io.github.malczuuu.modelmaker.PropertyFactory.property;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiUtilsTest {

  private static Property prop(boolean required, String description, String example) {
    return property(
        "id",
        PropType.ScalarType.STRING,
        required,
        Constraints.none(),
        "id",
        null,
        description,
        example);
  }

  @Test
  void nullWhenNeitherDescriptionNorExampleIsSet() {
    assertThat(OpenApiUtils.schemaAnnotationOf(prop(true, null, null))).isNull();
  }

  @Test
  void rendersDescriptionExampleAndRequiredModeInThatOrder() {
    AnnotationSpec spec = OpenApiUtils.schemaAnnotationOf(prop(true, "an id", "P1"));

    assertThat(spec).isNotNull();
    assertThat(spec.render())
        .isEqualTo(
            "@Schema(description = \"an id\", example = \"P1\","
                + " requiredMode = Schema.RequiredMode.REQUIRED)");
    assertThat(spec.getImportName()).isEqualTo("io.swagger.v3.oas.annotations.media.Schema");
  }

  @Test
  void optionalPropertyOmitsRequiredMode() {
    AnnotationSpec spec = OpenApiUtils.schemaAnnotationOf(prop(false, "an id", null));

    assertThat(spec).isNotNull();
    assertThat(spec.render()).isEqualTo("@Schema(description = \"an id\")");
  }

  @Test
  void escapesQuotesAndBackslashesInDescription() {
    AnnotationSpec spec = OpenApiUtils.schemaAnnotationOf(prop(false, "a \"b\" \\ c", null));

    assertThat(spec).isNotNull();
    assertThat(spec.render()).isEqualTo("@Schema(description = \"a \\\"b\\\" \\\\ c\")");
  }

  @Test
  void typeAnnotationIsNullWhenTypeHasNoDescription() {
    ModelType type =
        new ModelType("T", "com.example", null, List.of(), List.of(), FeatureOverrides.none());

    assertThat(OpenApiUtils.schemaAnnotationForType(type)).isNull();
  }

  @Test
  void typeAnnotationCarriesTheTypeDescription() {
    ModelType type =
        new ModelType(
            "T", "com.example", "A thing.", List.of(), List.of(), FeatureOverrides.none());

    AnnotationSpec spec = OpenApiUtils.schemaAnnotationForType(type);

    assertThat(spec).isNotNull();
    assertThat(spec.render()).isEqualTo("@Schema(description = \"A thing.\")");
  }
}
