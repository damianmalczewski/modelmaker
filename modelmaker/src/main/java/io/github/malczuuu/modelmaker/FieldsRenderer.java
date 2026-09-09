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
 * Renders the {@code private final} field declarations for one model type, one per property, each
 * preceded by the annotations of any feature whose config {@code emitsOnFields()}.
 *
 * <p>Renders just the fields; the enclosing class, its constructor and everything else are the
 * composing emitter's job.
 */
final class FieldsRenderer extends AbstractSnippetRenderer {

  private static final String NULLABLE_IMPORT = "org.jspecify.annotations.Nullable";

  /** Creates a new {@link FieldsRenderer}. */
  FieldsRenderer() {}

  /**
   * Renders the {@code private final} field declarations, one per property.
   *
   * @return the rendered fields, plus their imports.
   */
  @Override
  public RenderResult render() {
    Set<String> imports = new TreeSet<>();
    StringBuilder code = new StringBuilder();
    boolean openApi = options.getOpenApi().emitsOnFields();
    boolean jackson = options.getJackson().emitsOnFields();
    boolean validation = options.getValidation().emitsOnFields();
    for (Property p : properties) {
      for (String a :
          PropertyAnnotations.declarationAnnotations(p, openApi, jackson, validation, imports)) {
        code.append(indent).append(a).append('\n');
      }
      code.append(indent)
          .append("private final ")
          .append(fieldType(p, validation, imports))
          .append(' ')
          .append(p.getName())
          .append(";\n\n");
    }
    return new RenderResult(imports, code.toString());
  }

  /**
   * Type of the stored field: non-null when the value is always set, and a primitive scalar only
   * when {@link ModelOptions#isPreferPrimitives()} is on. A {@code List} field carries its
   * element's {@code jakarta.validation} constraints (and {@code @Valid} for a DTO element) in the
   * container-element position when {@code validation} annotates fields.
   *
   * @param prop the property to type.
   * @param validation whether {@code jakarta.validation} emits on fields.
   * @param imports import set to add any needed type to.
   * @return the rendered field type, including a leading {@code @Nullable} when applicable.
   */
  private String fieldType(Property prop, boolean validation, Set<String> imports) {
    if (prop.getType() instanceof PropType.ArrayType array) {
      String list =
          types.listType(
              array,
              PropertyAnnotations.elementAnnotations(prop, array, validation, imports),
              imports);
      if (prop.nonNull()) {
        return list;
      }
      imports.add(NULLABLE_IMPORT);
      return "@Nullable " + list;
    }
    if (prop.nonNull()) {
      return types.nonNull(prop.getType(), imports);
    }
    imports.add(NULLABLE_IMPORT);
    return "@Nullable " + types.boxed(prop.getType(), imports);
  }
}
