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

import java.util.Set;

/**
 * Renders the {@code builder()} static factory method for one model type: {@code public static
 * Builder builder() { return new Builder(); }}. The same for every type, so {@code init}'s {@code
 * type} and {@code options} arguments are ignored - only the indent matters.
 *
 * <p>Renders just this one method; the enclosing class, its nested {@code Builder} and everything
 * else are the composing emitter's job.
 */
final class BuilderFactoryRenderer implements SnippetRenderer {

  private String indent = "";

  /** Creates a new {@link BuilderFactoryRenderer}. */
  BuilderFactoryRenderer() {}

  /**
   * Records the indent; {@code type} and {@code options} are ignored, since {@code builder()}
   * renders identically for every model type.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type ignored.
   * @param options ignored.
   * @return {@code this}, so the call can chain into {@link #render()}.
   */
  @Override
  public SnippetRenderer init(String indent, ModelType type, ModelOptions options) {
    this.indent = indent;
    return this;
  }

  /**
   * Renders the {@code builder()} static factory method.
   *
   * @return the rendered code, with no imports.
   */
  @Override
  public RenderResult render() {
    String code =
        (indent + "public static Builder builder() {\n")
            + (indent + "  return new Builder();\n")
            + (indent + "}\n");
    return new RenderResult(Set.of(), code);
  }
}
