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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders the {@code private final} field declarations for one model type, one per property, each
 * preceded by its {@code jakarta.validation} constraint annotations when {@link
 * ModelOptions#isValidation()} is on.
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
    for (Property p : properties) {
      if (options.isValidation()) {
        for (String a : constraintAnnotations(p, imports)) {
          code.append(indent).append(a).append('\n');
        }
      }
      code.append(indent)
          .append("private final ")
          .append(fieldType(p, imports))
          .append(' ')
          .append(p.getName())
          .append(";\n\n");
    }
    return new RenderResult(imports, code.toString());
  }

  private List<String> constraintAnnotations(Property prop, Set<String> imports) {
    return ConstraintUtils.constraintAnnotationsOf(prop).stream()
        .map(
            spec -> {
              imports.add(spec.getImportName());
              return spec.getArgs().isEmpty()
                  ? "@" + spec.getSimpleName()
                  : "@" + spec.getSimpleName() + "(" + String.join(", ", spec.getArgs()) + ")";
            })
        .toList();
  }

  /**
   * Type of the stored field: non-null when the value is always set, and a primitive scalar only
   * when {@link ModelOptions#isPreferPrimitives()} is on. A {@code List} field carries its
   * element's {@code jakarta.validation} constraints (and {@code @Valid} for a DTO element) in the
   * container-element position - the field is the only position that does.
   *
   * @param prop the property to type.
   * @param imports import set to add any needed type to.
   * @return the rendered field type, including a leading {@code @Nullable} when applicable.
   */
  private String fieldType(Property prop, Set<String> imports) {
    if (prop.getType() instanceof PropType.ArrayType array) {
      String list = types.listType(array, elementAnnotations(prop, array, imports), imports);
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

  /**
   * Element annotations for a {@code List} field: {@code @Valid} for a generated-DTO element, then
   * the element's value constraints. Empty unless {@link ModelOptions#isValidation()} is on.
   *
   * @param prop the array property.
   * @param array its type.
   * @param imports import set to add each annotation's type to.
   * @return the rendered element annotations, in a stable order.
   */
  private List<String> elementAnnotations(
      Property prop, PropType.ArrayType array, Set<String> imports) {
    if (!options.isValidation()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    if (array.getItems() instanceof PropType.RefType) {
      imports.add("jakarta.validation.Valid");
      out.add("@Valid");
    }
    for (AnnotationSpec spec :
        ConstraintUtils.elementConstraintAnnotationsOf(
            prop.getConstraints().getElementConstraints())) {
      imports.add(spec.getImportName());
      out.add(
          spec.getArgs().isEmpty()
              ? "@" + spec.getSimpleName()
              : "@" + spec.getSimpleName() + "(" + String.join(", ", spec.getArgs()) + ")");
    }
    return out;
  }
}
