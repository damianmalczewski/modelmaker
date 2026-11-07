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

import java.util.List;
import java.util.Set;

/**
 * Renders the {@code mutate()} instance method for one model type: returns a {@code Builder}
 * pre-filled from every property's current value, via {@code builder().x(x).y(y)...}. {@code
 * init}'s {@code options} argument is ignored - nothing here is feature-gated.
 *
 * <p>Renders just this one method; the enclosing class, its nested {@code Builder} and everything
 * else are the composing emitter's job.
 */
final class MutateMethodRenderer implements SnippetRenderer {

  private String indent = "";
  private List<String> names = List.of();

  /** Creates a new {@link MutateMethodRenderer}. */
  MutateMethodRenderer() {}

  /**
   * Captures the indent and every property's name; {@code options} is ignored, since nothing here
   * is feature-gated.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render.
   * @param options ignored.
   * @return {@code this}, so the call can chain into {@link #render()}.
   */
  @Override
  public SnippetRenderer init(String indent, ModelType type, ModelOptions options) {
    this.indent = indent;
    this.names = type.getProperties().stream().map(Property::getName).toList();
    return this;
  }

  /**
   * Renders the {@code mutate()} instance method.
   *
   * @return the rendered method, with no imports.
   */
  @Override
  public RenderResult render() {
    StringBuilder code = new StringBuilder();
    code.append(indent).append("public Builder mutate() {\n");
    code.append(indent).append("  return builder()");
    for (String name : names) {
      // chain of builder assignments within `mutate` method
      code.append("\n")
          .append(indent)
          .append("      .")
          .append(name)
          .append('(')
          .append(name)
          .append(')');
    }
    code.append(";\n").append(indent).append("}\n\n");
    return new RenderResult(Set.of(), code.toString());
  }
}
