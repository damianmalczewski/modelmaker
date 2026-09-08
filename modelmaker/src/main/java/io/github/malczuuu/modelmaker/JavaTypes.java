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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Resolves a {@link PropType} to the Java type name it is emitted as, adding any needed import to a
 * caller-supplied set. Bound to one target package and one {@link ModelOptions} - the settings that
 * decide primitive vs boxed scalars. Container-element annotations ({@code @Valid} and element
 * constraints) are the caller's job - see {@link #listType(PropType.ArrayType, java.util.List,
 * java.util.Set)}.
 */
final class JavaTypes {

  private static final JavaTypes DEFAULT_VALUE = new JavaTypes("", ModelOptions.defaults());

  private final String targetPackage;
  private final ModelOptions options;

  /**
   * Creates a new {@link JavaTypes}.
   *
   * @param targetPackage the package the resolved model lives in, used to strip same-package {@link
   *     PropType.RefType} names down to their simple name.
   * @param options settings deciding primitive vs boxed scalars.
   */
  JavaTypes(String targetPackage, ModelOptions options) {
    this.targetPackage = targetPackage;
    this.options = options;
  }

  /**
   * The default instance - empty target package, {@link ModelOptions#defaults()}.
   *
   * @return the shared default instance.
   */
  static JavaTypes defaults() {
    return DEFAULT_VALUE;
  }

  /**
   * Non-null form: a primitive scalar when {@code preferPrimitives} is on, else the boxed name.
   *
   * @param type the property type to resolve.
   * @param imports import set to add any needed type to.
   * @return the rendered Java type name.
   */
  String nonNull(PropType type, Set<String> imports) {
    if (type instanceof PropType.ScalarType scalar) {
      return switch (scalar) {
        case STRING -> "String";
        case INTEGER -> options.isPreferPrimitives() ? "int" : "Integer";
        case LONG -> options.isPreferPrimitives() ? "long" : "Long";
        case NUMBER -> options.isPreferPrimitives() ? "double" : "Double";
        case FLOAT -> options.isPreferPrimitives() ? "float" : "Float";
        case BOOLEAN -> options.isPreferPrimitives() ? "boolean" : "Boolean";
        case BYTES -> "byte[]";
      };
    }
    if (type instanceof PropType.RefType ref) {
      return removePrefix(ref.getName(), targetPackage + ".");
    }
    if (type instanceof PropType.ExternalType external) {
      return externalType(external, imports);
    }
    return listType((PropType.ArrayType) type, imports);
  }

  /**
   * Always the boxed / reference form (never a primitive).
   *
   * @param type the property type to resolve.
   * @param imports import set to add any needed type to.
   * @return the rendered Java type name.
   */
  String boxed(PropType type, Set<String> imports) {
    if (type instanceof PropType.ScalarType scalar) {
      return switch (scalar) {
        case STRING -> "String";
        case INTEGER -> "Integer";
        case LONG -> "Long";
        case NUMBER -> "Double";
        case FLOAT -> "Float";
        case BOOLEAN -> "Boolean";
        case BYTES -> "byte[]";
      };
    }
    if (type instanceof PropType.RefType ref) {
      return removePrefix(ref.getName(), targetPackage + ".");
    }
    if (type instanceof PropType.ExternalType external) {
      return externalType(external, imports);
    }
    return listType((PropType.ArrayType) type, imports);
  }

  /**
   * Simple name of a JDK {@code $ref} target, importing it unless it lives in {@code java.lang}.
   *
   * @param type the external type to resolve.
   * @param imports import set to add the type to, unless it's in {@code java.lang}.
   * @return the type's simple name.
   */
  private String externalType(PropType.ExternalType type, Set<String> imports) {
    String qualifiedName = type.getQualifiedName();
    if (!qualifiedName.substring(0, qualifiedName.lastIndexOf('.')).equals("java.lang")) {
      imports.add(qualifiedName);
    }
    return type.simpleName();
  }

  private String listType(PropType.ArrayType type, Set<String> imports) {
    return listType(type, List.of(), imports);
  }

  /**
   * {@code List<...>}, with the given annotations placed on the element (container-element
   * position), e.g. {@code List<@Size(min = 1) @Valid Foo>}. Only {@link FieldsRenderer} passes
   * annotations - every other position renders the plain {@code List<T>}.
   *
   * @param type the array type to resolve.
   * @param elementAnnotations pre-rendered annotations (e.g. {@code "@Size(min = 1)"}) for the
   *     element, or empty.
   * @param imports import set to add any needed type to.
   * @return the rendered {@code List<...>} type.
   */
  String listType(PropType.ArrayType type, List<String> elementAnnotations, Set<String> imports) {
    imports.add("java.util.List");
    String prefix = elementAnnotations.isEmpty() ? "" : String.join(" ", elementAnnotations) + " ";
    return "List<" + prefix + elementType(type.getItems(), imports) + ">";
  }

  /**
   * Element type of a {@code List<...>}.
   *
   * @param type the element type to resolve.
   * @param imports import set to add any needed type to.
   * @return the rendered element type.
   */
  private String elementType(PropType type, Set<String> imports) {
    if (type instanceof PropType.ScalarType scalar) {
      return switch (scalar) {
        case STRING -> "String";
        case INTEGER -> "Integer";
        case LONG -> "Long";
        case NUMBER -> "Double";
        case FLOAT -> "Float";
        case BOOLEAN -> "Boolean";
        case BYTES -> "byte[]";
      };
    }
    if (type instanceof PropType.RefType ref) {
      return removePrefix(ref.getName(), targetPackage + ".");
    }
    if (type instanceof PropType.ExternalType external) {
      return externalType(external, imports);
    }
    PropType.ArrayType array = (PropType.ArrayType) type;
    return "List<" + elementType(array.getItems(), imports) + ">";
  }

  private static String removePrefix(String value, String prefix) {
    return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JavaTypes other)) {
      return false;
    }
    return Objects.equals(targetPackage, other.targetPackage)
        && Objects.equals(options, other.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetPackage, options);
  }

  @Override
  public String toString() {
    return "JavaTypes[" + ("targetPackage=" + targetPackage) + (", options=" + options) + "]";
  }
}
