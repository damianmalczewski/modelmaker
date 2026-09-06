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

class GettersRendererTest {

  private static final ModelOptions OPTIONS = ModelOptions.builder().build();
  private static final ModelOptions JACKSON = ModelOptions.builder().jackson(true).build();

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
  void rendersOneGetterPerProperty() {
    RenderResult result = new GettersRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "public String getId() {\n"
                + "  return id;\n"
                + "}\n\n"
                + "public @Nullable String getNote() {\n"
                + "  return note;\n"
                + "}\n\n");
    assertThat(result.getImports()).containsExactly("org.jspecify.annotations.Nullable");
  }

  @Test
  void aFieldLessTypeRendersNothing() {
    RenderResult result = new GettersRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getCode()).isEmpty();
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void jacksonOnAddsAJsonPropertyAnnotationWithTheOriginalJsonName() {
    ModelType type =
        type(
            "Widget",
            new Property(
                "userId", PropType.ScalarType.STRING, true, Constraints.none(), "user_id", null));

    RenderResult result = new GettersRenderer().init("", type, JACKSON).render();

    assertThat(result.getCode())
        .isEqualTo(
            "@JsonProperty(\"user_id\")\n"
                + "public String getUserId() {\n"
                + "  return userId;\n"
                + "}\n\n");
    assertThat(result.getImports())
        .containsExactly("com.fasterxml.jackson.annotation.JsonProperty");
  }

  @Test
  void aRequiredCollectionGetterReturnsAnUnmodifiableListUnconditionally() {
    ModelType type =
        type(
            "Bag",
            new Property(
                "tags",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                true,
                Constraints.none(),
                "tags",
                null));

    RenderResult result = new GettersRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode()).contains("  return Collections.unmodifiableList(tags);\n");
    assertThat(result.getImports()).contains("java.util.Collections");
  }

  @Test
  void anOptionalCollectionGetterStaysNullWhenUnset() {
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

    RenderResult result = new GettersRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode())
        .contains("  return tags != null ? Collections.unmodifiableList(tags) : null;\n");
    assertThat(result.getImports()).contains("java.util.Collections");
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new GettersRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
  }
}
