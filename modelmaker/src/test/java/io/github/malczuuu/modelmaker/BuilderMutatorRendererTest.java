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

class BuilderMutatorRendererTest {

  private static final ModelOptions OPTIONS = ModelOptions.builder().build();

  private static ModelType type(Property... properties) {
    return new ModelType(
        "Widget", "com.example.dto", null, List.of(properties), List.of(), FeatureOverrides.none());
  }

  private static final ModelType WIDGET =
      type(
          new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null),
          new Property("note", PropType.ScalarType.STRING, false, Constraints.none(), "note", null),
          new Property(
              "tags",
              PropType.ArrayType.of(PropType.ScalarType.STRING),
              false,
              Constraints.none(),
              "tags",
              null));

  @Test
  void rendersTheSealedInterfaceWithOneSetterPerField() {
    RenderResult result = new BuilderMutatorRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo(
            "public sealed interface BuilderMutator permits Builder {\n"
                + "\n"
                + "  BuilderMutator id(@Nullable String id);\n"
                + "\n"
                + "  BuilderMutator note(@Nullable String note);\n"
                + "\n"
                + "  BuilderMutator tags(@Nullable List<String> tags);\n"
                + "}\n");
  }

  @Test
  void everySetterParamIsNullableRegardlessOfRequired() {
    RenderResult result = new BuilderMutatorRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode()).contains("BuilderMutator id(@Nullable String id);");
  }

  @Test
  void reportsNullableAndFieldTypeImports() {
    RenderResult result = new BuilderMutatorRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getImports())
        .containsExactlyInAnyOrder("org.jspecify.annotations.Nullable", "java.util.List");
  }

  @Test
  void aFieldLessInterfaceNeedsNoNullableImport() {
    RenderResult result = new BuilderMutatorRenderer().init("", type(), OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo("public sealed interface BuilderMutator permits Builder {\n" + "}\n");
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new BuilderMutatorRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
    assertThat(result.getCode()).contains("    BuilderMutator id(@Nullable String id);");
  }

  @Test
  void rendersNoEnclosingClassOrImportLine() {
    RenderResult result = new BuilderMutatorRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode()).doesNotContain("class ").doesNotContain("import ");
  }
}
