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
 * Renders the {@code getXyz()} getters for one model type, one per property, each carrying
 * {@code @JsonProperty} when {@link ModelOptions#isJackson()} is on. A collection-valued getter
 * returns {@code Collections.unmodifiableList} of the field so a caller cannot mutate the DTO
 * through it.
 *
 * <p>Renders just the getters; the enclosing class, its fields and everything else are the
 * composing emitter's job.
 */
final class GettersRenderer extends AbstractSnippetRenderer {

  private static final String NULLABLE_IMPORT = "org.jspecify.annotations.Nullable";
  private static final String JSON_PROPERTY_IMPORT =
      "com.fasterxml.jackson.annotation.JsonProperty";
  private static final String COLLECTIONS_IMPORT = "java.util.Collections";

  /** Creates a new {@link GettersRenderer}. */
  GettersRenderer() {}

  /**
   * Renders the {@code getXyz()} getters, one per property.
   *
   * @return the rendered getters, plus their imports.
   */
  @Override
  public RenderResult render() {
    Set<String> imports = new TreeSet<>();
    StringBuilder code = new StringBuilder();
    for (Property p : properties) {
      if (options.isJackson()) {
        imports.add(JSON_PROPERTY_IMPORT);
        code.append(indent).append("@JsonProperty(\"").append(p.getJsonName()).append("\")\n");
      }
      code.append(indent)
          .append("public ")
          .append(fieldType(p, imports))
          .append(' ')
          .append(getterName(p))
          .append("() {\n");
      code.append(indent).append("  return ").append(getterReturn(p, imports)).append(";\n");
      code.append(indent).append("}\n\n");
    }
    return new RenderResult(imports, code.toString());
  }

  /**
   * Type of the getter return value: non-null when the value is always set, and a primitive scalar
   * only when {@link ModelOptions#isPreferPrimitives()} is on.
   *
   * @param prop the property to type.
   * @param imports import set to add any needed type to.
   * @return the rendered return type, including a leading {@code @Nullable} when applicable.
   */
  private String fieldType(Property prop, Set<String> imports) {
    if (prop.nonNull()) {
      return types.nonNull(prop.getType(), imports);
    }
    imports.add(NULLABLE_IMPORT);
    return "@Nullable " + types.boxed(prop.getType(), imports);
  }

  private static String getterName(Property prop) {
    return "get" + capitalize(prop.getName());
  }

  /**
   * Getter return expression: a collection-valued field is wrapped in {@code
   * Collections.unmodifiableList} so a caller cannot mutate the DTO through the getter; a
   * {@code @Nullable} collection stays {@code null} when unset.
   *
   * @param prop the property being returned.
   * @param imports import set to add {@code java.util.Collections} to when needed.
   * @return the rendered return expression.
   */
  private static String getterReturn(Property prop, Set<String> imports) {
    if (prop.getType() == PropType.ScalarType.BYTES) {
      String copy = prop.getName() + ".clone()";
      return prop.nonNull() ? copy : prop.getName() + " != null ? " + copy + " : null";
    }
    if (!(prop.getType() instanceof PropType.ArrayType)) {
      return prop.getName();
    }
    imports.add(COLLECTIONS_IMPORT);
    String wrapped = "Collections.unmodifiableList(" + prop.getName() + ")";
    return prop.nonNull() ? wrapped : prop.getName() + " != null ? " + wrapped + " : null";
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
