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

class WithersRendererTest {

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
  void rendersOneWitherPerPropertyReplacingJustThatArgument() {
    RenderResult result = new WithersRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "public Widget withId(String id) {\n"
                + "  return new Widget(Objects.requireNonNull(id), this.note);\n"
                + "}\n\n"
                + "public Widget withNote(@Nullable String note) {\n"
                + "  return new Widget(this.id, note);\n"
                + "}\n\n");
    assertThat(result.getImports())
        .containsExactlyInAnyOrder("java.util.Objects", "org.jspecify.annotations.Nullable");
  }

  @Test
  void aFieldLessTypeRendersNothing() {
    RenderResult result = new WithersRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getCode()).isEmpty();
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void aPreferredPrimitiveRequiredValueSkipsTheNullCheck() {
    ModelType type =
        type(
            "Config",
            new Property(
                "count", PropType.ScalarType.INTEGER, true, Constraints.none(), "count", null));
    ModelOptions primitives = ModelOptions.builder().preferPrimitives(true).build();

    RenderResult result = new WithersRenderer().init("", type, primitives).render();

    assertThat(result.getCode())
        .isEqualTo(
            "public Config withCount(int count) {\n" + "  return new Config(count);\n" + "}\n\n");
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new WithersRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
  }

  @Test
  void bytesWithersCopyTheReplacementArraySinceTheConstructorStoresItRaw() {
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

    RenderResult result = new WithersRenderer().init("", blob, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "public Blob withPayload(byte[] payload) {\n"
                + "  return new Blob(Objects.requireNonNull(payload).clone(), this.signature);\n"
                + "}\n\n"
                + "public Blob withSignature(@Nullable byte[] signature) {\n"
                + "  return new Blob(this.payload, signature != null ? signature.clone() : null);\n"
                + "}\n\n");
  }
}
