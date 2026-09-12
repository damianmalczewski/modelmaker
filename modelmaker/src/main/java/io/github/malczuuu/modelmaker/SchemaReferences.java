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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Validates {@link PropType.RefType} references across a set of {@link ModelType}s parsed from one
 * format's files - shared by {@link SimpleSchemaLoader} and {@link AvroSchemaLoader}. Each format
 * validates only within its own file set; there is no cross-format reference resolution.
 */
final class SchemaReferences {

  /**
   * Every {@link PropType.RefType} in {@code types} (including nested types, recursively) must
   * resolve to a top-level type's {@code packageName + "." + name} or a nested type's plain name.
   *
   * @param types the parsed top-level types to validate.
   */
  static void validate(List<ModelType> types) {
    // A `$ref` is a top-level FQCN; an inline nested type is referenced by its plain name.
    Set<String> known = new LinkedHashSet<>();
    for (ModelType t : types) {
      known.add(t.getPackageName() + "." + t.getName());
      collectNestedNames(t, known);
    }
    for (ModelType t : types) {
      validateRefs(t, known);
    }
  }

  private static void collectNestedNames(ModelType type, Set<String> sink) {
    for (ModelType n : type.getNested()) {
      sink.add(n.getName());
      collectNestedNames(n, sink);
    }
  }

  private static void validateRefs(ModelType type, Set<String> known) {
    for (Property prop : type.getProperties()) {
      String ref = refName(prop.getType());
      if (ref != null && !known.contains(ref)) {
        throw new SchemaException(
            type.getName()
                + "."
                + prop.getName()
                + ": $ref \""
                + ref
                + "\" does not match any schema title or nested type");
      }
    }
    for (ModelType n : type.getNested()) {
      validateRefs(n, known);
    }
  }

  private static @Nullable String refName(PropType type) {
    if (type instanceof PropType.RefType r) {
      return r.getName();
    }
    if (type instanceof PropType.ArrayType a) {
      return refName(a.getItems());
    }
    return null;
  }

  private SchemaReferences() {}
}
