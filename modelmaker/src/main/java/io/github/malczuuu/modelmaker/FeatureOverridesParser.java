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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Parses the top-level {@code "features"} object shared by both schema formats. Each entry is
 * itself an object: {@code jackson} / {@code validation} / {@code openApi} take {@code { enabled,
 * annotateFields, annotateGetters }}; {@code withers} / {@code preferPrimitives} take {@code {
 * enabled }}. An unset key falls through to the project default.
 */
final class FeatureOverridesParser {

  private static final Set<String> ANNOTATION_FEATURES = Set.of("jackson", "validation", "openApi");
  private static final Set<String> TOGGLE_FEATURES = Set.of("withers", "preferPrimitives");
  private static final Set<String> ANNOTATION_KEYS =
      Set.of("enabled", "annotateFields", "annotateGetters");
  private static final Set<String> TOGGLE_KEYS = Set.of("enabled");

  private final Function<String, RuntimeException> fail;

  /**
   * Creates a new parser.
   *
   * @param fail builds the loader's format-error exception from a message (the loader adds the file
   *     context).
   */
  FeatureOverridesParser(Function<String, RuntimeException> fail) {
    this.fail = fail;
  }

  /**
   * Parses one {@code "features"} object.
   *
   * @param features the object, or {@code null} when {@code "features"} is absent.
   * @return the parsed overrides ({@link FeatureOverrides#none()} when {@code features} is {@code
   *     null}).
   */
  FeatureOverrides parse(@Nullable JsonObject features) {
    if (features == null) {
      return FeatureOverrides.none();
    }
    for (String key : features.keySet()) {
      if (!ANNOTATION_FEATURES.contains(key) && !TOGGLE_FEATURES.contains(key)) {
        throw fail.apply("unknown \"features\" entry \"" + key + "\"");
      }
    }
    return FeatureOverrides.builder()
        .jackson(
            annotationOverride(features, "jackson", JacksonOverride.none(), JacksonOverride::new))
        .validation(
            annotationOverride(
                features, "validation", ValidationOverride.none(), ValidationOverride::new))
        .openApi(
            annotationOverride(features, "openApi", OpenApiOverride.none(), OpenApiOverride::new))
        .withers(toggleOverride(features, "withers"))
        .preferPrimitives(toggleOverride(features, "preferPrimitives"))
        .build();
  }

  /** Builds one feature's typed override from three {@code @Nullable Boolean} flags. */
  @FunctionalInterface
  private interface OverrideFactory<T> {
    T create(
        @Nullable Boolean enabled,
        @Nullable Boolean annotateFields,
        @Nullable Boolean annotateGetters);
  }

  private <T> T annotationOverride(
      JsonObject features, String feature, T none, OverrideFactory<T> factory) {
    if (member(features, feature) == null) {
      return none;
    }
    JsonObject obj = featureObject(features, feature, ANNOTATION_KEYS);
    return factory.create(
        boolFlag(obj, feature, "enabled"),
        boolFlag(obj, feature, "annotateFields"),
        boolFlag(obj, feature, "annotateGetters"));
  }

  private @Nullable Boolean toggleOverride(JsonObject features, String feature) {
    if (member(features, feature) == null) {
      return null;
    }
    return boolFlag(featureObject(features, feature, TOGGLE_KEYS), feature, "enabled");
  }

  private JsonObject featureObject(JsonObject features, String feature, Set<String> allowedKeys) {
    JsonElement node = member(features, feature);
    if (node == null || !node.isJsonObject()) {
      throw fail.apply(
          "\"features." + feature + "\" must be an object, e.g. { \"enabled\": true }");
    }
    JsonObject obj = node.getAsJsonObject();
    for (String key : obj.keySet()) {
      if (!allowedKeys.contains(key)) {
        throw fail.apply("unknown \"features." + feature + "\" entry \"" + key + "\"");
      }
    }
    return obj;
  }

  private @Nullable Boolean boolFlag(JsonObject obj, String feature, String key) {
    JsonElement n = member(obj, key);
    if (n == null) {
      return null;
    }
    if (!n.isJsonPrimitive() || !n.getAsJsonPrimitive().isBoolean()) {
      throw fail.apply("\"features." + feature + "." + key + "\" must be a boolean");
    }
    return n.getAsBoolean();
  }

  private static @Nullable JsonElement member(JsonObject node, String field) {
    JsonElement value = node.get(field);
    return value == null || value.isJsonNull() ? null : value;
  }
}
