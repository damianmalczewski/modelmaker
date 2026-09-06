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

class BuilderRendererTest {

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
  void rendersTheBuilderClass() {
    RenderResult result = new BuilderRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "@Generated(\"io.github.malczuuu.modelmaker\")\n"
                + "public static final class Builder implements BuilderMutator {\n"
                + "\n"
                + "  private Builder() {}\n"
                + "\n"
                + "  private @Nullable String id;\n"
                + "  private @Nullable String note;\n"
                + "\n"
                + "  @Override\n"
                + "  public Builder id(@Nullable String id) {\n"
                + "    this.id = id;\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @Override\n"
                + "  public Builder note(@Nullable String note) {\n"
                + "    this.note = note;\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @Override\n"
                + "  public String toString() {\n"
                + "    return \"Widget.Builder[\"\n"
                + "      + \"id=\" + id\n"
                + "      + \", note=\" + note\n"
                + "      + \"]\";\n"
                + "  }\n"
                + "\n"
                + "  public Widget build() {\n"
                + "    return new Widget(\n"
                + "        Objects.requireNonNull(id, \"id is required\"),\n"
                + "        note);\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  void requiredFieldsAreNullCheckedInBuildOthersFallThrough() {
    RenderResult result = new BuilderRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .contains("Objects.requireNonNull(id, \"id is required\")")
        .doesNotContain("requireNonNull(note");
  }

  @Test
  void reportsObjectsImportOnlyWhenAFieldIsRequired() {
    assertThat(new BuilderRenderer().init("", WIDGET, OPTIONS).render().getImports())
        .contains("java.util.Objects");

    ModelType optionalOnly =
        type(
            "Widget",
            new Property(
                "note", PropType.ScalarType.STRING, false, Constraints.none(), "note", null));
    assertThat(new BuilderRenderer().init("", optionalOnly, OPTIONS).render().getImports())
        .doesNotContain("java.util.Objects");
  }

  @Test
  void aFieldLessBuilderBuildsWithTheNoArgConstructor() {
    RenderResult result = new BuilderRenderer().init("", type("Empty"), OPTIONS).render();

    assertThat(result.getCode())
        .contains("public Empty build() {\n    return new Empty();\n")
        .contains("return \"Empty.Builder[]\";");
    assertThat(result.getImports()).containsExactly("javax.annotation.processing.Generated");
  }

  @Test
  void buildPassesAShallowCopyOfACollectionField() {
    ModelType type =
        type(
            "Bag",
            new Property(
                "tags",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                false,
                Constraints.none(),
                "tags",
                null),
            new Property(
                "ids",
                PropType.ArrayType.of(PropType.ScalarType.STRING),
                true,
                Constraints.none(),
                "ids",
                null));

    RenderResult result = new BuilderRenderer().init("", type, OPTIONS).render();

    assertThat(result.getCode())
        .contains("tags != null ? new ArrayList<>(tags) : null")
        .contains("new ArrayList<>(Objects.requireNonNull(ids, \"ids is required\"))");
    assertThat(result.getImports()).contains("java.util.ArrayList");
  }

  @Test
  void reportsArrayListImportOnlyWhenAFieldIsACollection() {
    assertThat(new BuilderRenderer().init("", WIDGET, OPTIONS).render().getImports())
        .doesNotContain("java.util.ArrayList");
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new BuilderRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
  }
}
