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
import org.jspecify.annotations.Nullable;

/**
 * A schema {@code default} value, validated against the property type. Only scalars and arrays of
 * scalars are supported.
 */
public sealed interface DefaultValue {

  /** A string default. */
  final class Str implements DefaultValue {

    private final String value;

    private Str(String value) {
      this.value = value;
    }

    /**
     * Creates a new {@link Str}.
     *
     * @param value the raw string, escaped by the emitter.
     * @return a new {@link Str}.
     */
    public static Str of(String value) {
      Objects.requireNonNull(value, "value must not be null");
      return new Str(value);
    }

    /**
     * The raw string, escaped by the emitter.
     *
     * @return the value.
     */
    public String getValue() {
      return value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Str other)) {
        return false;
      }
      return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

    @Override
    public String toString() {
      return "Str[value=" + value + "]";
    }
  }

  /** An {@code integer} or {@code number} default. */
  final class Num implements DefaultValue {

    private final String literal;

    private Num(String literal) {
      this.literal = literal;
    }

    /**
     * Creates a new {@link Num}.
     *
     * @param literal numeric literal text, e.g. {@code 42} or {@code 3.14}.
     * @return a new {@link Num}.
     */
    public static Num of(String literal) {
      Objects.requireNonNull(literal, "literal must not be null");
      return new Num(literal);
    }

    /**
     * Numeric literal text, e.g. {@code 42} or {@code 3.14}.
     *
     * @return the literal.
     */
    public String getLiteral() {
      return literal;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Num other)) {
        return false;
      }
      return Objects.equals(literal, other.literal);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(literal);
    }

    @Override
    public String toString() {
      return "Num[literal=" + literal + "]";
    }
  }

  /** A boolean default. */
  final class Bool implements DefaultValue {

    private final boolean value;

    private Bool(boolean value) {
      this.value = value;
    }

    /**
     * Creates a new {@link Bool}.
     *
     * @param value the boolean.
     * @return a new {@link Bool}.
     */
    public static Bool of(boolean value) {
      return new Bool(value);
    }

    /**
     * The boolean.
     *
     * @return the value.
     */
    public boolean getValue() {
      return value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Bool other)) {
        return false;
      }
      return value == other.value;
    }

    @Override
    public int hashCode() {
      return Boolean.hashCode(value);
    }

    @Override
    public String toString() {
      return "Bool[value=" + value + "]";
    }
  }

  /** An array-of-scalars default. */
  final class Arr implements DefaultValue {

    private final List<DefaultValue> elements;

    private Arr(List<DefaultValue> elements) {
      this.elements = elements;
    }

    /**
     * Creates a new {@link Arr}.
     *
     * @param elements the element defaults, each a scalar {@link DefaultValue}.
     * @return a new {@link Arr}.
     */
    public static Arr of(List<DefaultValue> elements) {
      Objects.requireNonNull(elements, "elements must not be null");
      return new Arr(elements);
    }

    /**
     * The element defaults, each a scalar {@link DefaultValue}.
     *
     * @return the elements.
     */
    public List<DefaultValue> getElements() {
      return elements;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Arr other)) {
        return false;
      }
      return Objects.equals(elements, other.elements);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(elements);
    }

    @Override
    public String toString() {
      return "Arr[elements=" + elements + "]";
    }
  }
}
