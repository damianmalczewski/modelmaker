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

/** A single property of a {@link ModelType}. */
public final class Property {

  private final String name;
  private final PropType type;
  private final boolean required;
  private final Constraints constraints;
  private final String jsonName;
  private final @Nullable DefaultValue defaultValue;

  /**
   * Creates a new {@link Property}.
   *
   * @param name camelCase identifier used for the generated field / getter / parameter.
   * @param type resolved property type.
   * @param required whether the JSON key is listed in the type's {@code required} array.
   * @param constraints validation keywords parsed from the property node.
   * @param jsonName original JSON key, kept for {@code @JsonProperty} and
   *     {@code @JsonPropertyOrder}.
   * @param defaultValue {@code default} value, or {@code null}.
   */
  Property(
      String name,
      PropType type,
      boolean required,
      Constraints constraints,
      String jsonName,
      @Nullable DefaultValue defaultValue) {
    this.name = name;
    this.type = type;
    this.required = required;
    this.constraints = constraints;
    this.jsonName = jsonName;
    this.defaultValue = defaultValue;
  }

  /**
   * camelCase identifier used for the generated field / getter / parameter.
   *
   * @return the name.
   */
  public String getName() {
    return name;
  }

  /**
   * Resolved property type.
   *
   * @return the type.
   */
  public PropType getType() {
    return type;
  }

  /**
   * Whether the JSON key is listed in the type's {@code required} array.
   *
   * @return {@code true} when required.
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Validation keywords parsed from the property node.
   *
   * @return the constraints.
   */
  public Constraints getConstraints() {
    return constraints;
  }

  /**
   * Original JSON key, kept for {@code @JsonProperty} and {@code @JsonPropertyOrder}; defaults to
   * {@link #getName()}.
   *
   * @return the JSON key.
   */
  public String getJsonName() {
    return jsonName;
  }

  /**
   * {@code default} value; when set the property is non-null (the default fills an absent value).
   *
   * @return the default value, or {@code null}.
   */
  public @Nullable DefaultValue getDefaultValue() {
    return defaultValue;
  }

  /**
   * Whether the property is always populated - {@link #isRequired()} or has a default.
   *
   * @return {@code true} when always populated.
   */
  public boolean nonNull() {
    return required || defaultValue != null;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Property other)) {
      return false;
    }
    return required == other.required
        && Objects.equals(name, other.name)
        && Objects.equals(type, other.type)
        && Objects.equals(constraints, other.constraints)
        && Objects.equals(jsonName, other.jsonName)
        && Objects.equals(defaultValue, other.defaultValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, required, constraints, jsonName, defaultValue);
  }

  @Override
  public String toString() {
    return "Property["
        + ("name=" + name)
        + (", type=" + type)
        + (", required=" + required)
        + (", constraints=" + constraints)
        + (", jsonName=" + jsonName)
        + (", defaultValue=" + defaultValue)
        + "]";
  }
}
