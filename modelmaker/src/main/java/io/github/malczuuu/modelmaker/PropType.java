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

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Resolved type of property, as parsed from a schema. */
public sealed interface PropType {

  /**
   * The JSON scalar types.
   *
   * <ul>
   *   <li>{@link #STRING} - JSON {@code "string"}
   *   <li>{@link #INTEGER} - JSON {@code "integer"}, {@code format: "int"} (the default) or absent
   *   <li>{@link #LONG} - JSON {@code "integer"}, {@code format: "long"}
   *   <li>{@link #NUMBER} - JSON {@code "number"}, {@code format: "double"} (the default) or absent
   *   <li>{@link #FLOAT} - JSON {@code "number"}, {@code format: "float"}
   *   <li>{@link #BOOLEAN} - JSON {@code "boolean"}
   *   <li>{@link #BYTES} - JSON {@code "bytes"} / Avro {@code "bytes"}; a {@code byte[]},
   *       serialized as a base64 string
   * </ul>
   */
  enum ScalarType implements PropType {

    /** JSON {@code "string"}. */
    STRING,

    /** JSON {@code "integer"}, {@code format: "int"} (the default) or absent. */
    INTEGER,

    /** JSON {@code "integer"}, {@code format: "long"}. */
    LONG,

    /** JSON {@code "number"}, {@code format: "double"} (the default) or absent. */
    NUMBER,

    /** JSON {@code "number"}, {@code format: "float"}. */
    FLOAT,

    /** JSON {@code "boolean"}. */
    BOOLEAN,

    /**
     * JSON {@code "bytes"} / Avro {@code "bytes"}. Emitted as a {@code byte[]} field with defensive
     * copies in every accessor, and serialized as a base64 string by Jackson.
     */
    BYTES
  }

  /** JSON {@code "array"}. */
  final class ArrayType implements PropType {

    private final PropType items;

    private ArrayType(PropType items) {
      this.items = items;
    }

    /**
     * Creates a new {@link ArrayType}.
     *
     * @param items element type; never another {@link ArrayType} (nested arrays are rejected).
     * @return a new {@link ArrayType}.
     */
    public static ArrayType of(PropType items) {
      Objects.requireNonNull(items, "items must not be null");
      return new ArrayType(items);
    }

    /**
     * Element type; never another {@link ArrayType} (nested arrays are rejected).
     *
     * @return the element type.
     */
    public PropType getItems() {
      return items;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ArrayType other)) {
        return false;
      }
      return Objects.equals(items, other.items);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(items);
    }

    @Override
    public String toString() {
      return "ArrayType[items=" + items + "]";
    }
  }

  /**
   * Reference to a top-level type (via {@code $ref}) or to an inline nested type by its simple
   * name.
   */
  final class RefType implements PropType {

    private final String name;

    private RefType(String name) {
      this.name = name;
    }

    /**
     * Creates a new {@link RefType}.
     *
     * @param name FQCN for a {@code $ref}, or the plain class name for a nested type.
     * @return a new {@link RefType}.
     */
    public static RefType of(String name) {
      Objects.requireNonNull(name, "name must not be null");
      return new RefType(name);
    }

    /**
     * FQCN for a {@code $ref}, or the plain class name for a nested type.
     *
     * @return the name.
     */
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof RefType other)) {
        return false;
      }
      return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name);
    }

    @Override
    public String toString() {
      return "RefType[name=" + name + "]";
    }
  }

  /**
   * {@code $ref} to a built-in JDK type ({@code java.*} / {@code javax.*}), e.g. {@code
   * java.time.Instant}. Emitted as its simple name with an import; no {@code @Valid} cascade.
   */
  final class ExternalType implements PropType {

    private final String qualifiedName;

    private ExternalType(String qualifiedName) {
      this.qualifiedName = qualifiedName;
    }

    /**
     * Creates a new {@link ExternalType}.
     *
     * @param qualifiedName the fully-qualified JDK class name from the {@code $ref}.
     * @return a new {@link ExternalType}.
     */
    public static ExternalType of(String qualifiedName) {
      Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
      return new ExternalType(qualifiedName);
    }

    /**
     * The fully-qualified JDK class name from the {@code $ref}.
     *
     * @return the qualified name.
     */
    public String getQualifiedName() {
      return qualifiedName;
    }

    /**
     * The last segment of {@link #getQualifiedName()}.
     *
     * @return the simple name.
     */
    public String simpleName() {
      return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ExternalType other)) {
        return false;
      }
      return Objects.equals(qualifiedName, other.qualifiedName);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(qualifiedName);
    }

    @Override
    public String toString() {
      return "ExternalType[qualifiedName=" + qualifiedName + "]";
    }
  }
}
