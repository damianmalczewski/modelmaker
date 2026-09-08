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

class ConstructorRendererTest {

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
  void rendersAPrivateAllArgsConstructor() {
    RenderResult result = new ConstructorRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "private Widget(\n"
                + "    String id,\n"
                + "    @Nullable String note) {\n"
                + "  this.id = id;\n"
                + "  this.note = note;\n"
                + "}\n");
    assertThat(result.getImports()).containsExactly("org.jspecify.annotations.Nullable");
  }

  @Test
  void aFieldLessTypeGetsAPrivateNoArgConstructor() {
    RenderResult result = new ConstructorRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getCode()).isEqualTo("private Empty() {}\n");
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void jacksonOnMakesItPackagePrivateWithJsonCreatorAndJsonProperty() {
    RenderResult result = new ConstructorRenderer().init("", WIDGET, JACKSON).render();

    assertThat(result.getCode())
        .isEqualTo(
            "@JsonCreator\n"
                + "Widget(\n"
                + "    @JsonProperty(\"id\") String id,\n"
                + "    @JsonProperty(\"note\") @Nullable String note) {\n"
                + "  this.id = id;\n"
                + "  this.note = note;\n"
                + "}\n");
    assertThat(result.getImports())
        .containsExactlyInAnyOrder(
            "com.fasterxml.jackson.annotation.JsonCreator",
            "com.fasterxml.jackson.annotation.JsonProperty",
            "org.jspecify.annotations.Nullable");
  }

  @Test
  void jacksonOnAddsJsonCreatorToTheNoArgConstructorToo() {
    RenderResult result = new ConstructorRenderer().init("", type("Empty"), JACKSON).render();

    assertThat(result.getCode()).isEqualTo("@JsonCreator\nEmpty() {}\n");
    assertThat(result.getImports()).containsExactly("com.fasterxml.jackson.annotation.JsonCreator");
  }

  @Test
  void aDefaultedOptionalPropertyFallsBackToItsDefaultWhenNull() {
    ModelType withDefault =
        type(
            "Config",
            new Property(
                "retries",
                PropType.ScalarType.INTEGER,
                false,
                Constraints.none(),
                "retries",
                DefaultValue.Num.of("3")));

    RenderResult result = new ConstructorRenderer().init("", withDefault, OPTIONS).render();

    assertThat(result.getCode()).contains("this.retries = retries != null ? retries : 3;");
  }

  @Test
  void anArrayDefaultRendersAsListOf() {
    ModelType withDefault =
        type(
            "Config",
            new Property(
                "roles",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                false,
                Constraints.none(),
                "roles",
                DefaultValue.Arr.of(List.of(DefaultValue.Str.of("user")))));

    RenderResult result = new ConstructorRenderer().init("", withDefault, OPTIONS).render();

    assertThat(result.getCode())
        .contains("this.roles = roles != null ? roles : List.of(\"user\");");
    assertThat(result.getImports())
        .containsExactlyInAnyOrder("org.jspecify.annotations.Nullable", "java.util.List");
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new ConstructorRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
  }

  @Test
  void bytesParametersAreStoredRawTheCopyIsTheBuildersAndWithersJob() {
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

    RenderResult result = new ConstructorRenderer().init("", blob, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "private Blob(\n"
                + "    byte[] payload,\n"
                + "    @Nullable byte[] signature) {\n"
                + "  this.payload = payload;\n"
                + "  this.signature = signature;\n"
                + "}\n");
  }
}
