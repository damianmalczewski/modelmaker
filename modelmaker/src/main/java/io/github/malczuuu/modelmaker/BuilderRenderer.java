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
 * Renders the {@code Builder} nested class for one model type - a mutable, prefix-free builder that
 * implements {@code BuilderMutator}; {@code build()} {@code Objects.requireNonNull}s every required
 * field, the rest fall through to the DTO constructor. A collection-valued field is passed to the
 * constructor as a shallow copy, so the built DTO never aliases the caller's collection.
 *
 * <p>Renders just the class; the enclosing class, the {@code package} declaration and the import
 * list are the composing emitter's job.
 */
final class BuilderRenderer extends AbstractSnippetRenderer {

  private static final String NULLABLE_IMPORT = "org.jspecify.annotations.Nullable";
  private static final String OBJECTS_IMPORT = "java.util.Objects";
  private static final String ARRAY_LIST_IMPORT = "java.util.ArrayList";
  private static final String BASE64_IMPORT = "java.util.Base64";

  private String builtType = "";

  /** Creates a new {@link BuilderRenderer}. */
  BuilderRenderer() {}

  /**
   * Captures the type's name, needed to name {@code build()}'s return type.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render; only its name is used here.
   * @param options ignored here - used by {@link #render()} via {@link #options}.
   */
  @Override
  protected void initInternal(String indent, ModelType type, ModelOptions options) {
    this.builtType = type.getName();
  }

  /**
   * Renders the {@code Builder} nested class.
   *
   * @return the rendered class, plus its imports.
   */
  @Override
  public RenderResult render() {
    String inner = indent + "  ";
    Set<String> imports = new TreeSet<>();
    if (properties.stream().anyMatch(p -> p.getType() == PropType.ScalarType.BYTES)) {
      imports.add(BASE64_IMPORT);
    }

    StringBuilder code = new StringBuilder();
    code.append(indent).append("public static final class Builder implements BuilderMutator {\n\n");
    code.append(inner).append("private Builder() {}\n\n");

    for (Property property : properties) {
      imports.add(NULLABLE_IMPORT);
      code.append(inner)
          .append("private @Nullable ")
          .append(types.boxed(property.getType(), imports))
          .append(' ')
          .append(property.getName())
          .append(";\n");
    }
    if (!properties.isEmpty()) {
      code.append('\n');
    }

    for (Property property : properties) {
      code.append(inner).append("@Override\n");
      code.append(inner)
          .append("public Builder ")
          .append(property.getName())
          .append("(@Nullable ")
          .append(types.boxed(property.getType(), imports))
          .append(' ')
          .append(property.getName())
          .append(") {\n");
      String name = property.getName();
      String assigned = name;
      if (property.getType() == PropType.ScalarType.BYTES) {
        // Defensive copy on the way in, so a later change to the caller's array can't leak through.
        assigned = name + " != null ? " + name + ".clone() : null";
      }
      code.append(inner)
          .append("  this.")
          .append(name)
          .append(" = ")
          .append(assigned)
          .append(";\n");
      code.append(inner).append("  return this;\n");
      code.append(inner).append("}\n\n");
    }

    code.append(toStringMethod(inner)).append('\n');

    code.append(inner).append("public ").append(builtType).append(" build() {\n");
    if (properties.isEmpty()) {
      code.append(inner).append("  return new ").append(builtType).append("();\n");
    } else {
      code.append(inner).append("  return new ").append(builtType).append("(\n");
      for (int i = 0; i < properties.size(); i++) {
        Property property = properties.get(i);
        String name = property.getName();
        String value;
        if (property.isRequired()) {
          imports.add(OBJECTS_IMPORT);
          value = "Objects.requireNonNull(" + name + ", \"" + name + " is required\")";
        } else {
          value = name;
        }
        if (property.getType() instanceof PropType.ArrayType) {
          imports.add(ARRAY_LIST_IMPORT);
          value =
              property.isRequired()
                  ? "new ArrayList<>(" + value + ")"
                  : name + " != null ? new ArrayList<>(" + name + ") : null";
        }
        code.append(inner)
            .append("      ")
            .append(value)
            .append(i == properties.size() - 1 ? ");\n" : ",\n");
      }
    }
    code.append(inner).append("}\n");
    code.append(indent).append("}\n");

    return new RenderResult(imports, code.toString());
  }

  /**
   * {@code toString()} of the form {@code "Widget.Builder[" + ("a=" + a) + (", b=" + b) + "]"}.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @return the rendered method.
   */
  private String toStringMethod(String indent) {
    StringBuilder parts = new StringBuilder();
    for (int i = 0; i < properties.size(); i++) {
      if (i > 0) {
        parts.append("\n").append(indent).append("    + ");
      }
      Property property = properties.get(i);
      String name = property.getName();
      // The builder field is always a @Nullable byte[], so the base64 term is null-guarded.
      String value =
          property.getType() == PropType.ScalarType.BYTES
              ? "("
                  + name
                  + " != null ? Base64.getEncoder().encodeToString("
                  + name
                  + ") : \"null\")"
              : name;
      parts
          .append(i == 0 ? "\"" + name + "=\"" : "\", " + name + "=\"")
          .append(" + ")
          .append(value);
    }
    String label = builtType + ".Builder";
    String body =
        properties.isEmpty()
            ? "\"" + label + "[]\""
            : "\"" + label + "[\"\n" + indent + "    + " + parts + "\n" + indent + "    + \"]\"";
    return (indent + "@Override\n")
        + (indent + "public String toString() {\n")
        + (indent + "  return " + body + ";\n")
        + (indent + "}\n");
  }
}
