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

import org.junit.jupiter.api.Test;

class MutateExtensionRendererTest {

  @Test
  void rendersJustTheFunctionForATopLevelType() {
    RenderResult result = new MutateExtensionRenderer().init("", "Widget").render();

    assertThat(result.getCode())
        .isEqualTo(
            "@Generated(\"io.github.malczuuu.modelmaker\")\n"
                + "public inline fun Widget.mutate(block: Widget.BuilderMutator.() -> Unit):"
                + " Widget =\n"
                + "    this.mutate().also { it.block() }.build()\n");
  }

  @Test
  void reportsTheGeneratedImportAndNothingElse() {
    RenderResult result = new MutateExtensionRenderer().init("", "Widget").render();

    assertThat(result.getImports()).containsExactly("javax.annotation.processing.Generated");
  }

  @Test
  void dotQualifiesANestedReceiver() {
    RenderResult result = new MutateExtensionRenderer().init("", "Order.Line").render();

    assertThat(result.getCode())
        .contains(
            "public inline fun Order.Line.mutate(block: Order.Line.BuilderMutator.() -> Unit):"
                + " Order.Line =");
  }

  @Test
  void prependsTheIndentToEveryLine() {
    RenderResult result = new MutateExtensionRenderer().init("  ", "Widget").render();

    assertThat(result.getCode().lines()).allMatch(line -> line.isEmpty() || line.startsWith("  "));
    assertThat(result.getCode()).contains("      this.mutate().also { it.block() }.build()");
  }

  @Test
  void emitsNoPackageOrImportLine() {
    RenderResult result = new MutateExtensionRenderer().init("", "Widget").render();

    assertThat(result.getCode()).doesNotContain("package ").doesNotContain("import ");
  }
}
