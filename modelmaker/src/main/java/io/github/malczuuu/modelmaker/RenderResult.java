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

import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Output of a single {@code *SnippetRenderer}: a chunk of generated source ({@link #getCode()})
 * plus the fully-qualified type names it references ({@link #getImports()}), which the composing
 * emitter collects into the file's import list.
 */
final class RenderResult {

  private final Set<String> imports;
  private final String code;

  /**
   * Creates a new {@link RenderResult}.
   *
   * @param imports fully-qualified type names the rendered code references.
   * @param code the rendered source chunk.
   */
  RenderResult(Set<String> imports, String code) {
    this.imports = imports;
    this.code = code;
  }

  /**
   * Fully-qualified type names the rendered code references.
   *
   * @return the imports.
   */
  Set<String> getImports() {
    return imports;
  }

  /**
   * The rendered source chunk.
   *
   * @return the code.
   */
  String getCode() {
    return code;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RenderResult other)) {
      return false;
    }
    return Objects.equals(imports, other.imports) && Objects.equals(code, other.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imports, code);
  }

  @Override
  public String toString() {
    return "RenderResult[" + ("imports=" + imports) + (", code=" + code) + "]";
  }
}
