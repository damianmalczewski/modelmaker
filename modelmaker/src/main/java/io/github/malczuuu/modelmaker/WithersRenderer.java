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
 * Renders the {@code withXyz(...)} copy methods for one model type, one per property. Each returns
 * a new instance built through the all-args constructor with just that one field replaced; a
 * non-null, non-primitive replacement value is {@code Objects.requireNonNull}-checked.
 *
 * <p>Renders just the withers; the enclosing class, its constructor and everything else are the
 * composing emitter's job - including whether to call this renderer at all, gated by {@link
 * ModelOptions#isWithers()}.
 */
final class WithersRenderer extends AbstractSnippetRenderer {

  private static final String NULLABLE_IMPORT = "org.jspecify.annotations.Nullable";
  private static final String OBJECTS_IMPORT = "java.util.Objects";

  private String typeName = "";

  /** Creates a new {@link WithersRenderer}. */
  WithersRenderer() {}

  /**
   * Captures the type's name, needed to name each wither's return type.
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
   * Renders the {@code withXyz(...)} copy methods, one per property.
   *
   * @return the rendered withers, plus their imports.
   */
  @Override
  public RenderResult render() {
    Set<String> imports = new TreeSet<>();
    StringBuilder code = new StringBuilder();
    for (int pi = 0; pi < properties.size(); pi++) {
      Property p = properties.get(pi);
      StringBuilder args = new StringBuilder();
      for (int i = 0; i < properties.size(); i++) {
        Property it = properties.get(i);
        if (i > 0) {
          args.append(", ");
        }
        if (i != pi) {
          args.append("this.").append(it.getName());
        } else if (it.nonNull() && !isPrimitiveScalar(it)) {
          imports.add(OBJECTS_IMPORT);
          args.append("Objects.requireNonNull(").append(p.getName()).append(")");
        } else {
          args.append(p.getName());
        }
      }
      code.append(indent)
          .append("public ")
          .append(typeName)
          .append(" with")
          .append(capitalize(p.getName()))
          .append('(')
          .append(fieldType(p, imports))
          .append(' ')
          .append(p.getName())
          .append(") {\n");
      code.append(indent)
          .append("  return new ")
          .append(typeName)
          .append('(')
          .append(args)
          .append(");\n");
      code.append(indent).append("}\n\n");
    }
    return new RenderResult(imports, code.toString());
  }

  /**
   * A scalar field emitted as a Java primitive (so {@code Objects.requireNonNull} does not apply).
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

  /**
   * Type of the wither parameter: non-null when the value is always set, and a primitive scalar
   * only when {@link ModelOptions#isPreferPrimitives()} is on.
   *
   * @param prop the property to type.
   * @param imports import set to add any needed type to.
   * @return the rendered parameter type, including a leading {@code @Nullable} when applicable.
   */
  private String fieldType(Property prop, Set<String> imports) {
    if (prop.nonNull()) {
      return types.nonNull(prop.getType(), imports);
    }
    imports.add(NULLABLE_IMPORT);
    return "@Nullable " + types.boxed(prop.getType(), imports);
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
