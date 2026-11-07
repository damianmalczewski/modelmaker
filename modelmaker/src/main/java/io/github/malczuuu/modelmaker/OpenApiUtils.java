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
import org.jspecify.annotations.Nullable;

/**
 * Maps schema documentation metadata to the OpenAPI {@code
 * io.swagger.v3.oas.annotations.media.Schema} annotation. Only carries what {@code
 * jakarta.validation} cannot express - {@code description}, {@code example} and, for a required
 * property, {@code requiredMode} - so a model with both the {@code validation} and {@code openApi}
 * features on does not repeat every facet.
 */
final class OpenApiUtils {

  static final String SCHEMA_IMPORT = "io.swagger.v3.oas.annotations.media.Schema";

  private OpenApiUtils() {}

  /**
   * The {@code @Schema} annotation for one property, or {@code null} when the property carries no
   * {@code description} / {@code example} (a bare {@code requiredMode} is left to
   * {@code @NotNull}).
   *
   * @param prop the property to map.
   * @return the annotation, or {@code null}.
   */
  static @Nullable AnnotationSpec schemaAnnotationOf(Property prop) {
    String description = prop.getDescription();
    String example = prop.getExample();
    if (description == null && example == null) {
      return null;
    }
    List<String> args = new ArrayList<>();
    if (description != null) {
      args.add("description = \"" + ConstraintUtils.escapeConstraintLiteral(description) + "\"");
    }
    if (example != null) {
      args.add("example = \"" + ConstraintUtils.escapeConstraintLiteral(example) + "\"");
    }
    if (prop.isRequired()) {
      args.add("requiredMode = Schema.RequiredMode.REQUIRED");
    }
    return new AnnotationSpec("Schema", SCHEMA_IMPORT, List.copyOf(args));
  }

  /**
   * The {@code @Schema} annotation for a model type, or {@code null} when the type has no {@code
   * description}.
   *
   * @param type the model type to map.
   * @return the annotation, or {@code null}.
   */
  static @Nullable AnnotationSpec schemaAnnotationForType(ModelType type) {
    String description = type.getDescription();
    if (description == null) {
      return null;
    }
    return new AnnotationSpec(
        "Schema",
        SCHEMA_IMPORT,
        List.of("description = \"" + ConstraintUtils.escapeConstraintLiteral(description) + "\""));
  }
}
