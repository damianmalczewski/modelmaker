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

class FieldsRendererTest {

  private static final ModelOptions OPTIONS = ModelOptions.builder().build();

  private static ModelType type(String name, Property... properties) {
    return new ModelType(
        name, "com.example.dto", null, List.of(properties), List.of(), FeatureOverrides.none());
  }

  private static final ModelType WIDGET =
      type(
          "Widget",
          new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null),
          new Property(
              "note", PropType.ScalarType.STRING, false, Constraints.none(), "note", null));

  @Test
  void rendersOnePrivateFinalFieldPerProperty() {
    RenderResult result = new FieldsRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo("private final String id;\n\nprivate final @Nullable String note;\n\n");
    assertThat(result.getImports()).containsExactly("org.jspecify.annotations.Nullable");
  }

  @Test
  void aFieldLessTypeRendersNothing() {
    RenderResult result = new FieldsRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getCode()).isEmpty();
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void aCollectionFieldReportsTheListImport() {
    ModelType type =
        type(
            "Bag",
            new Property(
                "tags",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                false,
                Constraints.none(),
                "tags",
                null));

    RenderResult result = new FieldsRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode()).contains("private final @Nullable List<String> tags;");
    assertThat(result.getImports())
        .containsExactlyInAnyOrder("org.jspecify.annotations.Nullable", "java.util.List");
  }

  @Test
  void validationOnPrependsConstraintAnnotationsBeforeTheField() {
    ModelType type =
        type(
            "Person",
            new Property(
                "id",
                PropType.ScalarType.STRING,
                true,
                Constraints.builder().pattern("^X\\d+$").build(),
                "id",
                null));
    ModelOptions validation = ModelOptions.builder().validation(true).build();

    RenderResult result = new FieldsRenderer().init("", type, validation).render();

    assertThat(result.getCode())
        .contains("@Pattern(regexp = \"^X\\\\d+$\", message = \"must match \\\"^X\\\\d+$\\\"\")")
        .contains("private final String id;");
    assertThat(result.getImports()).contains("jakarta.validation.constraints.Pattern");
  }

  @Test
  void openapiOnPrependsSchemaAnnotationWithDescriptionAndExample() {
    ModelType type =
        type(
            "Person",
            new Property(
                "id",
                PropType.ScalarType.STRING,
                true,
                Constraints.none(),
                "id",
                null,
                "the identifier",
                "P123"));
    ModelOptions openapi = ModelOptions.builder().openapi(true).build();

    RenderResult result = new FieldsRenderer().init("", type, openapi).render();

    assertThat(result.getCode())
        .contains(
            "@Schema(description = \"the identifier\", example = \"P123\","
                + " requiredMode = Schema.RequiredMode.REQUIRED)")
        .contains("private final String id;");
    assertThat(result.getImports()).contains("io.swagger.v3.oas.annotations.media.Schema");
  }

  @Test
  void openapiOnEmitsNoSchemaAnnotationWithoutDescriptionOrExample() {
    ModelType type =
        type(
            "Person",
            new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null));
    ModelOptions openapi = ModelOptions.builder().openapi(true).build();

    RenderResult result = new FieldsRenderer().init("", type, openapi).render();

    assertThat(result.getCode()).doesNotContain("@Schema");
    assertThat(result.getImports()).doesNotContain("io.swagger.v3.oas.annotations.media.Schema");
  }

  @Test
  void openapiOffEmitsNoSchemaAnnotations() {
    ModelType type =
        type(
            "Person",
            new Property(
                "id",
                PropType.ScalarType.STRING,
                true,
                Constraints.none(),
                "id",
                null,
                "the identifier",
                null));

    RenderResult result = new FieldsRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode()).doesNotContain("@Schema");
  }

  @Test
  void validationOffEmitsNoConstraintAnnotations() {
    ModelType type =
        type(
            "Person",
            new Property(
                "id",
                PropType.ScalarType.STRING,
                true,
                Constraints.builder().pattern("^X\\d+$").build(),
                "id",
                null));

    RenderResult result = new FieldsRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode()).doesNotContain("@Pattern").contains("private final String id;");
  }

  @Test
  void validationOnPutsElementConstraintsOnTheListField() {
    ModelType type =
        type(
            "Bag",
            new Property(
                "tags",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                false,
                Constraints.builder()
                    .maxItems(3)
                    .elementConstraints(Constraints.builder().minLength(1).maxLength(255).build())
                    .build(),
                "tags",
                null));
    ModelOptions validation = ModelOptions.builder().validation(true).build();

    RenderResult result = new FieldsRenderer().init("", type, validation).render();

    assertThat(result.getCode())
        .contains("@Size(max = 3, message = \"size must be at most 3\")")
        .contains(
            "private final @Nullable List<@Size(min = 1, max = 255, message = \"size must be"
                + " between 1 and 255\") String> tags;");
    assertThat(result.getImports()).contains("jakarta.validation.constraints.Size");
  }

  @Test
  void validationOnCascadesValidToADtoListElementOnTheFieldOnly() {
    ModelType type =
        type(
            "Bag",
            new Property(
                "addresses",
                PropType.ArrayType.of(PropType.RefType.of("Address")),
                true,
                Constraints.none(),
                "addresses",
                null));
    ModelOptions validation = ModelOptions.builder().validation(true).build();

    RenderResult result = new FieldsRenderer().init("", type, validation).render();

    assertThat(result.getCode()).contains("private final List<@Valid Address> addresses;");
    assertThat(result.getImports()).contains("jakarta.validation.Valid");
  }

  @Test
  void validationOffLeavesTheListElementPlain() {
    ModelType type =
        type(
            "Bag",
            new Property(
                "tags",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                false,
                Constraints.builder()
                    .elementConstraints(Constraints.builder().minLength(1).build())
                    .build(),
                "tags",
                null));

    RenderResult result = new FieldsRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode())
        .doesNotContain("@Size")
        .contains("private final @Nullable List<String> tags;");
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new FieldsRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
  }

  @Test
  void bytesRenderAsAByteArrayField() {
    ModelType blob =
        type(
            "Blob",
            new Property(
                "payload", PropType.ScalarType.BYTES, true, Constraints.none(), "payload", null),
            new Property(
                "signature",
                PropType.ScalarType.BYTES,
                false,
                Constraints.none(),
                "signature",
                null));

    RenderResult result = new FieldsRenderer().init("", blob, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "private final byte[] payload;\n\nprivate final @Nullable byte[] signature;\n\n");
  }
}
