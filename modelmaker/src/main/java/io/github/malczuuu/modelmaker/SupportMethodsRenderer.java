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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders {@code equals}, {@code hashCode} and {@code toString} for one model type, each over every
 * property in declaration order. Always rendered - unlike the other renderers, nothing here is
 * gated by a {@link ModelOptions} feature flag; {@code options} is only consulted to decide whether
 * a primitive scalar field compares with {@code ==} instead of {@code Objects.equals} in the
 * generated {@code equals()}.
 *
 * <p>Renders just these three methods; the enclosing class and everything else are the composing
 * emitter's job.
 */
final class SupportMethodsRenderer implements SnippetRenderer {

  private String indent = "";
  private String typeName = "";
  private List<Property> properties = List.of();
  private ModelOptions options = ModelOptions.defaults();

  /** Creates a new {@link SupportMethodsRenderer}. */
  SupportMethodsRenderer() {}

  /**
   * Captures the indent, the type's name, every property and the options - the latter only used to
   * decide, per property, whether {@code equals()} compares with {@code ==} or {@code
   * Objects.equals}.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render.
   * @param options settings deciding which fields are primitive scalars.
   * @return {@code this}, so the call can chain into {@link #render()}.
   */
  @Override
  public SnippetRenderer init(String indent, ModelType type, ModelOptions options) {
    this.indent = indent;
    this.typeName = type.getName();
    this.properties = type.getProperties();
    this.options = options;
    return this;
  }

  /**
   * Renders {@code equals}, {@code hashCode} and {@code toString}, each over every property.
   *
   * @return the rendered methods, plus their imports.
   */
  @Override
  public RenderResult render() {
    Set<String> imports = new TreeSet<>();
    imports.add("java.util.Objects");
    imports.add("org.jspecify.annotations.Nullable");
    String code = equalsMethod() + hashCodeMethod() + toStringMethod();
    return new RenderResult(imports, code);
  }

  private String equalsMethod() {
    String comparisons =
        properties.isEmpty()
            ? "true"
            : properties.stream()
                .map(this::equalsComparison)
                .collect(joining("\n" + indent + "    && "));
    return (indent + "@Override\n")
        + (indent + "public boolean equals(@Nullable Object obj) {\n")
        + (indent + "  if (this == obj) {\n")
        + (indent + "    return true;\n")
        + (indent + "  }\n")
        + (indent + "  if (!(obj instanceof " + typeName + " other)) {\n")
        + (indent + "    return false;\n")
        + (indent + "  }\n")
        + (indent + "  return " + comparisons + ";\n")
        + (indent + "}\n\n");
  }

  /**
   * The {@code equals()} comparison for one property: {@code ==} for a primitive scalar (avoids
   * autoboxing, and is exact for {@code int} / {@code boolean}), {@code Objects.equals} otherwise.
   *
   * @param prop the property to compare.
   * @return the rendered comparison expression.
   */
  private String equalsComparison(Property prop) {
    String n = prop.getName();
    return isPrimitiveScalar(prop)
        ? n + " == other." + n
        : "Objects.equals(" + n + ", other." + n + ")";
  }

  /**
   * A scalar field emitted as a Java primitive.
   *
   * @param prop the property to check.
   * @return {@code true} when {@code prop} is a primitive scalar.
   */
  private boolean isPrimitiveScalar(Property prop) {
    return options.isPreferPrimitives()
        && prop.nonNull()
        && (prop.getType() == PropType.ScalarType.INTEGER
            || prop.getType() == PropType.ScalarType.LONG
            || prop.getType() == PropType.ScalarType.NUMBER
            || prop.getType() == PropType.ScalarType.FLOAT
            || prop.getType() == PropType.ScalarType.BOOLEAN);
  }

  private String hashCodeMethod() {
    String names = properties.stream().map(Property::getName).collect(joining(", "));
    return (indent + "@Override\n")
        + (indent + "public int hashCode() {\n")
        + (indent + "  return Objects.hash(" + names + ");\n")
        + (indent + "}\n\n");
  }

  private String toStringMethod() {
    StringBuilder parts = new StringBuilder();
    for (int i = 0; i < properties.size(); i++) {
      if (i > 0) {
        parts.append("\n").append(indent).append("    + ");
      }
      String n = properties.get(i).getName();
      parts.append(i == 0 ? "\"" + n + "=\"" : "\", " + n + "=\"").append(" + ").append(n);
    }
    String body =
        properties.isEmpty()
            ? "\"" + typeName + "[]\""
            : "\"" + typeName + "[\"\n" + indent + "    + " + parts + "\n" + indent + "    + \"]\"";
    return (indent + "@Override\n")
        + (indent + "public String toString() {\n")
        + (indent + "  return " + body + ";\n")
        + (indent + "}\n");
  }
}
