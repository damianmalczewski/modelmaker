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
import java.util.TreeSet;

/**
 * Renders the {@code BuilderMutator} sealed interface for one model type - every fluent setter, no
 * {@code build()}, every method returning {@code BuilderMutator} so a {@code Mutator} reference can
 * never reach {@code build()}.
 *
 * <p>Renders just the interface; the enclosing class, the {@code package} declaration and the
 * import list are the composing emitter's job.
 */
final class BuilderMutatorRenderer extends AbstractSnippetRenderer {

  private static final String NULLABLE_IMPORT = "org.jspecify.annotations.Nullable";

  /** Creates a new {@link BuilderMutatorRenderer}. */
  BuilderMutatorRenderer() {}

  /**
   * Renders the {@code BuilderMutator} interface, one setter method per property.
   *
   * @return the rendered code, plus its imports.
   */
  @Override
  public RenderResult render() {
    String inner = indent + "  ";
    Set<String> imports = new TreeSet<>();

    StringBuilder code = new StringBuilder();
    code.append(indent).append("public sealed interface BuilderMutator permits Builder {\n");
    for (Property property : properties) {
      imports.add(NULLABLE_IMPORT);
      code.append('\n')
          .append(inner)
          .append("BuilderMutator ")
          .append(property.getName())
          .append("(@Nullable ")
          .append(types.boxed(property.getType(), imports))
          .append(' ')
          .append(property.getName())
          .append(");\n");
    }
    code.append(indent).append("}\n");

    return new RenderResult(imports, code.toString());
  }
}
