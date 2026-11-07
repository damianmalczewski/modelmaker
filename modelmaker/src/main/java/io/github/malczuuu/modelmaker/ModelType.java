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
 * One model class to be generated - a top-level schema or an inline nested type. Produced by {@link
 * SchemaLoader}; only {@link #getName()} and {@link #getPackageName()} are part of the public
 * surface.
 */
public final class ModelType {

  private final String name;
  private final String packageName;
  private final @Nullable String description;
  private final List<Property> properties;
  private final List<ModelType> nested;
  private final FeatureOverrides featureOverrides;

  /**
   * Creates a new {@link ModelType}.
   *
   * @param name simple class name.
   * @param packageName package of the emitted type.
   * @param description schema {@code description}, or {@code null}.
   * @param properties the class's properties, in schema order.
   * @param nested inline {@code type: object} definitions, emitted as nested classes.
   * @param featureOverrides the top-level schema's {@code "features"} overrides.
   */
  ModelType(
      String name,
      String packageName,
      @Nullable String description,
      List<Property> properties,
      List<ModelType> nested,
      FeatureOverrides featureOverrides) {
    this.name = name;
    this.packageName = packageName;
    this.description = description;
    this.properties = properties;
    this.nested = nested;
    this.featureOverrides = featureOverrides;
  }

  /**
   * Simple class name.
   *
   * @return the name.
   */
  public String getName() {
    return name;
  }

  /**
   * Package of the emitted type; a nested type carries its enclosing type's package.
   *
   * @return the package name.
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Schema {@code description}; parsed but not currently emitted.
   *
   * @return the description, or {@code null}.
   */
  public @Nullable String getDescription() {
    return description;
  }

  /**
   * The class's properties, in schema order.
   *
   * @return the properties.
   */
  public List<Property> getProperties() {
    return properties;
  }

  /**
   * Inline {@code type: object} definitions, emitted as nested classes of this type.
   *
   * @return the nested types.
   */
  public List<ModelType> getNested() {
    return nested;
  }

  /**
   * The top-level schema's {@code "features"} object, each flag overriding the matching {@code
   * modelmaker.features} default for this file only. Only meaningful on a top-level type - a nested
   * type always carries the default overrides.
   *
   * @return the feature overrides.
   */
  public FeatureOverrides getFeatureOverrides() {
    return featureOverrides;
  }

  /**
   * Copies this type with a different {@link #getFeatureOverrides()}.
   *
   * @param featureOverrides the replacement overrides.
   * @return a new {@link ModelType} with every other field unchanged.
   */
  ModelType withFeatureOverrides(FeatureOverrides featureOverrides) {
    return new ModelType(name, packageName, description, properties, nested, featureOverrides);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ModelType other)) {
      return false;
    }
    return Objects.equals(name, other.name)
        && Objects.equals(packageName, other.packageName)
        && Objects.equals(description, other.description)
        && Objects.equals(properties, other.properties)
        && Objects.equals(nested, other.nested)
        && Objects.equals(featureOverrides, other.featureOverrides);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, packageName, description, properties, nested, featureOverrides);
  }

  @Override
  public String toString() {
    return "ModelType["
        + ("name=" + name)
        + (", packageName=" + packageName)
        + (", description=" + description)
        + (", properties=" + properties)
        + (", nested=" + nested)
        + (", featureOverrides=" + featureOverrides)
        + "]";
  }
}
