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

class BuilderFactoryRendererTest {

  private static final ModelOptions OPTIONS = ModelOptions.builder().build();
  private static final ModelType WIDGET =
      new ModelType(
          "Widget", "com.example.dto", null, List.of(), List.of(), FeatureOverrides.none());

  @Test
  void rendersTheStaticFactoryMethod() {
    RenderResult result = new BuilderFactoryRenderer().init("", WIDGET, OPTIONS).render();

    assertThat(result.getCode())
        .isEqualTo("public static Builder builder() {\n  return new Builder();\n}\n");
    assertThat(result.getImports()).isEmpty();
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new BuilderFactoryRenderer().init("  ", WIDGET, OPTIONS).render();

    assertThat(result.getCode().lines()).allMatch(line -> line.startsWith("  "));
  }

  @Test
  void implementsTheRendererContract() {
    assertThat(new BuilderFactoryRenderer().init("", WIDGET, OPTIONS))
        .isInstanceOf(SnippetRenderer.class);
  }
}
