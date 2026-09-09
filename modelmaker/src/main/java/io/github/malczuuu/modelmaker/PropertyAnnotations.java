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

/**
 * Shared assembly of the per-property annotations that {@link FieldsRenderer} and {@link
 * GettersRenderer} place on their declaration - which are emitted, and where, depends on each
 * feature config's {@code enabled} / {@code annotateFields} / {@code annotateGetters} flags.
 */
final class PropertyAnnotations {

  private static final String JSON_PROPERTY_IMPORT =
      "com.fasterxml.jackson.annotation.JsonProperty";
  private static final String VALID_IMPORT = "jakarta.validation.Valid";

  private PropertyAnnotations() {}

  /**
   * Annotation lines placed directly before a field or getter declaration for {@code prop}: the
   * OpenAPI {@code @Schema}, the Jackson {@code @JsonProperty}, then the {@code jakarta.validation}
   * constraints - each included only when that feature emits at this site.
   *
   * @param prop the property.
   * @param openApi whether OpenAPI emits at this site.
   * @param jackson whether Jackson emits at this site.
   * @param validation whether {@code jakarta.validation} emits at this site.
   * @param imports import set to add every emitted annotation's type to.
   * @return the rendered annotation lines, in a stable order.
   */
  static List<String> declarationAnnotations(
      Property prop, boolean openApi, boolean jackson, boolean validation, Set<String> imports) {
    List<String> out = new ArrayList<>();
    if (openApi) {
      AnnotationSpec schema = OpenApiUtils.schemaAnnotationOf(prop);
      if (schema != null) {
        imports.add(schema.getImportName());
        out.add(schema.render());
      }
    }
    if (jackson) {
      imports.add(JSON_PROPERTY_IMPORT);
      out.add("@JsonProperty(\"" + prop.getJsonName() + "\")");
    }
    if (validation) {
      for (AnnotationSpec spec : ConstraintUtils.constraintAnnotationsOf(prop)) {
        imports.add(spec.getImportName());
        out.add(spec.render());
      }
    }
    return out;
  }

  /**
   * Container-element annotations for a {@code List}-typed {@code prop} - {@code @Valid} for a
   * generated-DTO element, then the element's value constraints - or empty when {@code validation}
   * does not emit at this site.
   *
   * @param prop the array property.
   * @param array its type.
   * @param validation whether {@code jakarta.validation} emits at this site.
   * @param imports import set to add each annotation's type to.
   * @return the rendered element annotations, in a stable order.
   */
  static List<String> elementAnnotations(
      Property prop, PropType.ArrayType array, boolean validation, Set<String> imports) {
    if (!validation) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    if (array.getItems() instanceof PropType.RefType) {
      imports.add(VALID_IMPORT);
      out.add("@Valid");
    }
    for (AnnotationSpec spec :
        ConstraintUtils.elementConstraintAnnotationsOf(
            prop.getConstraints().getElementConstraints())) {
      imports.add(spec.getImportName());
      out.add(spec.render());
    }
    return out;
  }
}
