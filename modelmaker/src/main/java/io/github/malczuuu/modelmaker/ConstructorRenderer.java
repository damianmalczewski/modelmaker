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

import static java.util.stream.Collectors.joining;

import java.util.Set;
import java.util.TreeSet;

/**
 * Renders the all-args constructor for one model type (no-arg when it has no properties).
 * Package-private with {@code @JsonCreator}/{@code @JsonProperty} when {@link
 * ModelOptions#isJackson()} is on, so Jackson deserializes through it directly instead of through
 * the {@code Builder}; otherwise private. A defaulted property's parameter falls back to its {@code
 * default} when {@code null}.
 *
 * <p>Renders just the constructor; the enclosing class, its fields and everything else are the
 * composing emitter's job.
 */
final class ConstructorRenderer extends AbstractSnippetRenderer {

  private static final String NULLABLE_IMPORT = "org.jspecify.annotations.Nullable";
  private static final String JSON_CREATOR_IMPORT = "com.fasterxml.jackson.annotation.JsonCreator";
  private static final String JSON_PROPERTY_IMPORT =
      "com.fasterxml.jackson.annotation.JsonProperty";

  private String typeName = "";

  /** Creates a new {@link ConstructorRenderer}. */
  ConstructorRenderer() {}

  /**
   * Captures the type's name, needed to name the constructor.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render; only its name is used here.
   * @param options ignored here - used by {@link #render()} via {@link #options}.
   */
  @Override
  protected void initInternal(String indent, ModelType type, ModelOptions options) {
    this.typeName = type.getName();
  }

  /**
   * Renders the all-args constructor (no-arg when the type has no properties).
   *
   * @return the rendered constructor, plus its imports.
   */
  @Override
  public RenderResult render() {
    Set<String> imports = new TreeSet<>();
    StringBuilder code = new StringBuilder();
    String visibility = options.isJackson() ? "" : "private ";

    if (properties.isEmpty()) {
      if (options.isJackson()) {
        imports.add(JSON_CREATOR_IMPORT);
        code.append(indent).append("@JsonCreator\n");
      }
      code.append(indent).append(visibility).append(typeName).append("() {}\n");
      return new RenderResult(imports, code.toString());
    }

    if (options.isJackson()) {
      imports.add(JSON_CREATOR_IMPORT);
      code.append(indent).append("@JsonCreator\n");
    }
    code.append(indent).append(visibility).append(typeName).append("(\n");
    for (int i = 0; i < properties.size(); i++) {
      Property p = properties.get(i);
      code.append(indent).append("    ");
      if (options.isJackson()) {
        imports.add(JSON_PROPERTY_IMPORT);
        code.append("@JsonProperty(\"").append(p.getJsonName()).append("\") ");
      }
      code.append(paramType(p, imports))
          .append(' ')
          .append(p.getName())
          .append(i == properties.size() - 1 ? ") {\n" : ",\n");
    }
    for (Property p : properties) {
      DefaultValue d = p.getDefaultValue();
      String rhs =
          d != null
              ? p.getName() + " != null ? " + p.getName() + " : " + renderDefault(d, p.getType())
              : p.getName();
      code.append(indent)
          .append("  this.")
          .append(p.getName())
          .append(" = ")
          .append(rhs)
          .append(";\n");
    }
    code.append(indent).append("}\n");

    return new RenderResult(imports, code.toString());
  }

  /**
   * Type of the constructor parameter: a primitive scalar only for a {@code required} value when
   * {@code preferPrimitives} is on; a defaulted or optional value takes a {@code @Nullable} box so
   * a {@code null} from an absent field maps to the default.
   *
   * @param prop the property to type.
   * @param imports import set to add any needed type to.
   * @return the rendered parameter type, including a leading {@code @Nullable} when applicable.
   */
  private String paramType(Property prop, Set<String> imports) {
    if (prop.isRequired()) {
      return types.nonNull(prop.getType(), imports);
    }
    imports.add(NULLABLE_IMPORT);
    return "@Nullable " + types.boxed(prop.getType(), imports);
  }

  /**
   * Renders a default value literal. {@code type} decides the Java literal suffix a numeric default
   * needs: {@code L} for a {@link PropType.ScalarType#LONG} field (a bare int literal wouldn't
   * compile for a value past {@code Integer.MAX_VALUE}), {@code f} for a {@link
   * PropType.ScalarType#FLOAT} field (a bare decimal literal is a {@code double}, and assigning
   * that to a {@code float} without the suffix is a lossy-conversion compile error).
   *
   * @param value the default to render.
   * @param type the property's (or, recursively, the array element's) type.
   * @return the rendered literal.
   */
  private String renderDefault(DefaultValue value, PropType type) {
    if (value instanceof DefaultValue.Str s) {
      return "\"" + escapeJava(s.getValue()) + "\"";
    }
    if (value instanceof DefaultValue.Num n) {
      String literal = n.getLiteral();
      if (type == PropType.ScalarType.LONG) {
        return literal + "L";
      }
      if (type == PropType.ScalarType.FLOAT) {
        return literal + "f";
      }
      return literal;
    }
    if (value instanceof DefaultValue.Bool b) {
      return Boolean.toString(b.getValue());
    }
    DefaultValue.Arr arr = (DefaultValue.Arr) value;
    PropType itemType = ((PropType.ArrayType) type).getItems();
    return "List.of("
        + arr.getElements().stream().map(e -> renderDefault(e, itemType)).collect(joining(", "))
        + ")";
  }

  private static String escapeJava(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
