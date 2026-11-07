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
import static io.github.malczuuu.modelmaker.PropertyFactory.sensitiveProperty;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SupportMethodsRendererTest {

  private static final ModelOptions OPTIONS = ModelOptions.builder().build();

  private static ModelType type(String name, Property... properties) {
    return new ModelType(
        name, "com.example.dto", null, List.of(properties), List.of(), FeatureOverrides.none());
  }

  private static final ModelType WIDGET =
      type(
          "Widget",
          property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null),
          property("note", PropType.ScalarType.STRING, false, Constraints.none(), "note", null));

  @Test
  void masksASensitivePropertyInToStringOnly() {
    ModelType widget =
        type(
            "Widget",
            property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null),
            sensitiveProperty(
                "token", PropType.ScalarType.STRING, true, Constraints.none(), "token", null));

    RenderResult result = new SupportMethodsRenderer().init("", widget, OPTIONS).render();

    assertThat(result.getCode()).contains("\", token=***\"").doesNotContain("\", token=\" + token");
    // equals / hashCode keep comparing the real value.
    assertThat(result.getCode())
        .contains("Objects.equals(token, other.token)")
        .contains("Objects.hash(id, token)");
  }

  @Test
  void rendersEqualsHashCodeAndToString() {
    RenderResult result = new SupportMethodsRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "@Override\n"
                + "public boolean equals(@Nullable Object obj) {\n"
                + "  if (this == obj) {\n"
                + "    return true;\n"
                + "  }\n"
                + "  if (!(obj instanceof Widget other)) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  return Objects.equals(id, other.id)\n"
                + "    && Objects.equals(note, other.note);\n"
                + "}\n\n"
                + "@Override\n"
                + "public int hashCode() {\n"
                + "  return Objects.hash(id, note);\n"
                + "}\n\n"
                + "@Override\n"
                + "public String toString() {\n"
                + "  return \"Widget[\"\n"
                + "    + \"id=\" + id\n"
                + "    + \", note=\" + note\n"
                + "    + \"]\";\n"
                + "}\n");
    assertThat(result.getImports())
        .containsExactlyInAnyOrder("java.util.Objects", "org.jspecify.annotations.Nullable");
  }

  @Test
  void aFieldLessTypeComparesTrueAndPrintsAnEmptyBody() {
    RenderResult result = new SupportMethodsRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getCode())
        .contains("return true;")
        .contains("return Objects.hash();")
        .contains("return \"Empty[]\";");
  }

  @Test
  void alwaysReportsObjectsAndNullableRegardlessOfProperties() {
    RenderResult result = new SupportMethodsRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getImports())
        .containsExactlyInAnyOrder("java.util.Objects", "org.jspecify.annotations.Nullable");
  }

  @Test
  void comparesPrimitiveScalarFieldsWithDoubleEqualsWhenPreferPrimitivesIsOn() {
    ModelOptions preferPrimitives = ModelOptions.builder().preferPrimitives(true).build();
    ModelType type =
        type(
            "Widget",
            property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null),
            property("count", PropType.ScalarType.INTEGER, true, Constraints.none(), "count", null),
            property("ratio", PropType.ScalarType.NUMBER, true, Constraints.none(), "ratio", null),
            property("on", PropType.ScalarType.BOOLEAN, true, Constraints.none(), "on", null));

    RenderResult result = new SupportMethodsRenderer().init("", type, preferPrimitives).render();

    assertThat(result.getCode())
        .contains("Objects.equals(id, other.id)")
        .contains("count == other.count")
        .contains("ratio == other.ratio")
        .contains("on == other.on")
        .doesNotContain("Objects.equals(count, other.count)")
        .doesNotContain("Objects.equals(ratio, other.ratio)")
        .doesNotContain("Objects.equals(on, other.on)");
  }

  @Test
  void comparesOptionalScalarFieldsWithObjectsEqualsEvenWhenPreferPrimitivesIsOn() {
    ModelOptions preferPrimitives = ModelOptions.builder().preferPrimitives(true).build();
    ModelType type =
        type(
            "Widget",
            property(
                "count", PropType.ScalarType.INTEGER, false, Constraints.none(), "count", null));

    RenderResult result = new SupportMethodsRenderer().init("", type, preferPrimitives).render();

    assertThat(result.getCode()).contains("Objects.equals(count, other.count)");
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new SupportMethodsRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
  }

  @Test
  void bytesFieldsCompareAndHashByContentAndPrintAsBase64() {
    ModelType blob =
        type(
            "Blob",
            property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null),
            property(
                "payload", PropType.ScalarType.BYTES, true, Constraints.none(), "payload", null),
            property("sig", PropType.ScalarType.BYTES, false, Constraints.none(), "sig", null));

    RenderResult result = new SupportMethodsRenderer().init("", blob, OPTIONS).render();

    assertThat(result.getCode())
        .contains("&& Arrays.equals(payload, other.payload)")
        .contains("&& Arrays.equals(sig, other.sig);")
        .contains("return Objects.hash(id, Arrays.hashCode(payload), Arrays.hashCode(sig));")
        .contains("+ \", payload=\" + Base64.getEncoder().encodeToString(payload)")
        .contains(
            "+ \", sig=\" + (sig != null ? Base64.getEncoder().encodeToString(sig) : \"null\")");
    assertThat(result.getImports()).contains("java.util.Arrays", "java.util.Base64");
  }
}
