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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** {@link SchemaLoader} that parses the schema files. */
public final class SimpleSchemaLoader implements SchemaLoader {

  /** Creates a new {@link SimpleSchemaLoader}. */
  public SimpleSchemaLoader() {}

  /** The only schema format {@code "$modelmaker"} version this loader accepts, for now. */
  private static final String SUPPORTED_VERSION = "v1.0";

  private static final Pattern LEADING_UNDERSCORES = Pattern.compile("^_*");
  private static final Pattern CAMEL_CASE = Pattern.compile("[a-z][a-zA-Z0-9]*");
  private static final Pattern SNAKE_CASE = Pattern.compile("[a-z][a-z0-9]*(_[a-z0-9]+)*");
  private static final Pattern UPPER_CAMEL_CASE = Pattern.compile("[A-Z][a-zA-Z0-9]*");

  /**
   * {@code @Pattern} (regex, message) per {@code string} {@code format}. {@code decimal} also
   * enables {@code minimum}/{@code maximum} as {@code @DecimalMin}/{@code @DecimalMax}; {@code
   * email} is handled separately as {@code @Email}.
   */
  private record Format(String pattern, String message) {}

  private static final Map<String, Format> STRING_FORMATS =
      Map.of(
          "decimal", new Format("^-?\\d+(\\.\\d+)?$", "must be a decimal number"),
          "date", new Format("^\\d{4}-\\d{2}-\\d{2}$", "must be a date in yyyy-MM-dd format"),
          "time",
              new Format("^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$", "must be a time in HH:mm:ss format"),
          "date-time",
              new Format(
                  "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$",
                  "must be an ISO-8601 date-time"),
          "uuid",
              new Format(
                  "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                  "must be a UUID"));

  /**
   * Accepts only {@code "json"} files.
   *
   * @param extension a file extension, case-insensitively compared.
   * @return {@code true} for {@code "json"}.
   */
  @Override
  public boolean canLoad(String extension) {
    return "json".equalsIgnoreCase(extension);
  }

  /**
   * Parses each file independently, then validates every {@code $ref} across all of them.
   *
   * @param files schema files to parse; processing order is by file name, independent of the list
   *     order.
   * @return one {@link ModelType} per file, with all {@code $ref}s resolved and validated.
   */
  @Override
  public List<ModelType> load(List<Path> files) {
    Objects.requireNonNull(files, "files must not be null");

    List<Path> sorted = new ArrayList<>(files);
    sorted.sort(Comparator.comparing(SimpleSchemaLoader::fileName));
    List<ModelType> types = new ArrayList<>();
    for (Path f : sorted) {
      types.add(parseFile(f));
    }
    SchemaReferences.validate(types);
    return List.copyOf(types);
  }

  private static ModelType parseFile(Path file) {
    JsonElement parsed;
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      parsed = JsonParser.parseReader(reader);
    } catch (Exception e) {
      throw new IllegalStateException(
          fileName(file) + ": not valid JSON (" + e.getMessage() + ")", e);
    }
    JsonObject root = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();

    String version = stringOrNull(root, "$modelmaker");
    if (version == null) {
      throw fail(file, "missing \"$modelmaker\"");
    }
    if (!SUPPORTED_VERSION.equals(version)) {
      throw fail(
          file,
          "unsupported \"$modelmaker\": \""
              + version
              + "\" (only \""
              + SUPPORTED_VERSION
              + "\" is"
              + " supported)");
    }

    String title = stringOrNull(root, "title");
    if (title == null) {
      throw fail(file, "missing \"title\"");
    }
    if (!title.contains(".")) {
      throw fail(
          file, "\"title\" must be a fully-qualified class name, e.g. \"com.example.dto.Foo\"");
    }
    if (!fileName(file).equals(title + ".json")) {
      throw fail(file, "file must be named \"" + title + ".json\" to match its title");
    }
    if (!"object".equals(stringOrNull(root, "type"))) {
      throw fail(file, "top-level \"type\" must be \"object\"");
    }
    JsonElement featuresNode = get(root, "features");
    JsonObject features =
        featuresNode != null && featuresNode.isJsonObject() ? featuresNode.getAsJsonObject() : null;
    FeatureOverrides overrides =
        FeatureOverrides.builder()
            .jackson(parseFeatureFlag(file, features, "jackson"))
            .validation(parseFeatureFlag(file, features, "validation"))
            .withers(parseFeatureFlag(file, features, "withers"))
            .preferPrimitives(parseFeatureFlag(file, features, "preferPrimitives"))
            .openapi(parseFeatureFlag(file, features, "openapi"))
            .build();

    return parseObject(
        file,
        root,
        title.substring(title.lastIndexOf('.') + 1),
        title.substring(0, title.lastIndexOf('.')),
        overrides);
  }

  /**
   * Reads {@code features.<field>} ({@code true}/{@code false}), a per-schema override of {@code
   * modelmaker.features} for this file only. {@code null} when the top-level {@code "features"}
   * object, or this field of it, is absent - the project default then applies.
   *
   * @param file the schema file being parsed, for error messages.
   * @param features the top-level {@code "features"} object, or {@code null} when absent.
   * @param field the flag name to read.
   * @return the flag's value, or {@code null} when unset.
   */
  private static @Nullable Boolean parseFeatureFlag(
      Path file, @Nullable JsonObject features, String field) {
    if (features == null) {
      return null;
    }
    JsonElement fieldNode = get(features, field);
    if (fieldNode == null) {
      return null;
    }
    if (!isBoolean(fieldNode)) {
      throw fail(file, "\"features." + field + "\" must be a boolean");
    }
    return fieldNode.getAsBoolean();
  }

  private static ModelType parseObject(
      Path file,
      JsonObject node,
      String name,
      String packageName,
      FeatureOverrides featureOverrides) {
    Set<String> required = new LinkedHashSet<>();
    JsonElement requiredNode = get(node, "required");
    if (requiredNode != null && requiredNode.isJsonArray()) {
      for (JsonElement n : requiredNode.getAsJsonArray()) {
        if (isString(n)) {
          required.add(n.getAsString());
        }
      }
    }
    JsonElement propertiesNode = get(node, "properties");
    if (propertiesNode == null || !propertiesNode.isJsonObject()) {
      throw fail(file, "type \"" + name + "\": missing \"properties\" object");
    }

    List<ModelType> nested = new ArrayList<>();
    List<Property> properties = new ArrayList<>();
    Map<String, String> identifiers = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : propertiesNode.getAsJsonObject().entrySet()) {
      String jsonKey = entry.getKey();
      JsonElement propValue = entry.getValue();
      JsonObject propNode =
          propValue.isJsonObject() ? propValue.getAsJsonObject() : new JsonObject();
      String identifier = toIdentifier(file, name, jsonKey);
      String clashingKey = identifiers.putIfAbsent(identifier, jsonKey);
      if (clashingKey != null) {
        throw fail(
            file,
            "type \""
                + name
                + "\": properties \""
                + clashingKey
                + "\" and \""
                + jsonKey
                + "\" both map to the field name \""
                + identifier
                + "\"");
      }
      PropType type = parseType(file, identifier, packageName, propNode, nested);
      boolean isRequired = required.contains(jsonKey);
      DefaultValue defaultValue =
          isRequired ? null : parseDefault(file, identifier, propNode, type);
      properties.add(
          new Property(
              identifier,
              type,
              isRequired,
              parseConstraints(propNode, type),
              jsonKey,
              defaultValue,
              stringOrNull(propNode, "description"),
              exampleStringOrNull(propNode)));
    }
    return new ModelType(
        name,
        packageName,
        stringOrNull(node, "description"),
        List.copyOf(properties),
        List.copyOf(nested),
        featureOverrides);
  }

  private static PropType parseType(
      Path file, String nameHint, String packageName, JsonObject node, List<ModelType> nestedSink) {
    String ref = stringOrNull(node, "$ref");
    if (ref != null) {
      return isBuiltInJavaType(ref) ? PropType.ExternalType.of(ref) : PropType.RefType.of(ref);
    }
    String t = stringOrNull(node, "type");
    if (t == null) {
      throw fail(file, "property \"" + nameHint + "\": needs \"type\" or \"$ref\"");
    }
    return switch (t) {
      case "string" -> PropType.ScalarType.STRING;
      case "bytes" -> PropType.ScalarType.BYTES;
      case "integer" -> integerFormat(file, nameHint, node);
      case "number" -> numberFormat(file, nameHint, node);
      case "boolean" -> PropType.ScalarType.BOOLEAN;
      case "object" -> {
        String nestedTitle = stringOrNull(node, "title");
        if (nestedTitle != null && nestedTitle.contains(".")) {
          throw fail(
              file,
              "nested object \"title\" must be a plain class name, not \"" + nestedTitle + "\"");
        }
        String nestedName = nestedTitle != null ? nestedTitle : capitalize(nameHint);
        nestedSink.add(parseObject(file, node, nestedName, packageName, FeatureOverrides.none()));
        yield PropType.RefType.of(nestedName);
      }
      case "array" -> {
        JsonElement items = get(node, "items");
        if (items == null || !items.isJsonObject()) {
          throw fail(file, "property \"" + nameHint + "\": array needs \"items\"");
        }
        PropType itemType =
            parseType(file, nameHint, packageName, items.getAsJsonObject(), nestedSink);
        if (itemType instanceof PropType.ArrayType) {
          throw fail(file, "property \"" + nameHint + "\": nested arrays are not supported");
        }
        yield PropType.ArrayType.of(itemType);
      }
      default ->
          throw fail(
              file,
              "property \""
                  + nameHint
                  + "\": unsupported type \""
                  + t
                  + "\" (allowed: string, bytes, integer, number, boolean, object, array, $ref)");
    };
  }

  /**
   * {@code "integer"}'s {@code format}: {@code "int"} (the default, absent included) or {@code
   * "long"}.
   *
   * @param file the schema file being parsed, for error messages.
   * @param nameHint the property name, for error messages.
   * @param node the property node.
   * @return {@link PropType.ScalarType#INTEGER} or {@link PropType.ScalarType#LONG}.
   */
  private static PropType.ScalarType integerFormat(Path file, String nameHint, JsonObject node) {
    String format = stringOrNull(node, "format");
    if (format == null || format.equals("int")) {
      return PropType.ScalarType.INTEGER;
    }
    if (format.equals("long")) {
      return PropType.ScalarType.LONG;
    }
    throw fail(
        file,
        "property \""
            + nameHint
            + "\": unsupported format \""
            + format
            + "\" for type \"integer\" (allowed: int, long)");
  }

  /**
   * {@code "number"}'s {@code format}: {@code "double"} (the default, absent included) or {@code
   * "float"}.
   *
   * @param file the schema file being parsed, for error messages.
   * @param nameHint the property name, for error messages.
   * @param node the property node.
   * @return {@link PropType.ScalarType#NUMBER} or {@link PropType.ScalarType#FLOAT}.
   */
  private static PropType.ScalarType numberFormat(Path file, String nameHint, JsonObject node) {
    String format = stringOrNull(node, "format");
    if (format == null || format.equals("double")) {
      return PropType.ScalarType.NUMBER;
    }
    if (format.equals("float")) {
      return PropType.ScalarType.FLOAT;
    }
    throw fail(
        file,
        "property \""
            + nameHint
            + "\": unsupported format \""
            + format
            + "\" for type \"number\" (allowed: float, double)");
  }

  private static @Nullable DefaultValue parseDefault(
      Path file, String prop, JsonObject node, PropType type) {
    JsonElement d = get(node, "default");
    return d == null ? null : toDefaultValue(file, prop, d, type);
  }

  private static DefaultValue toDefaultValue(
      Path file, String prop, JsonElement node, PropType type) {
    if (type == PropType.ScalarType.STRING) {
      if (!isString(node)) {
        throw fail(file, "property \"" + prop + "\": default must be a string");
      }
      return DefaultValue.Str.of(node.getAsString());
    }
    if (type == PropType.ScalarType.INTEGER || type == PropType.ScalarType.LONG) {
      if (!isIntegralNumber(node)) {
        throw fail(file, "property \"" + prop + "\": default must be an integer");
      }
      return DefaultValue.Num.of(Long.toString(node.getAsLong()));
    }
    if (type == PropType.ScalarType.NUMBER || type == PropType.ScalarType.FLOAT) {
      if (!isNumber(node)) {
        throw fail(file, "property \"" + prop + "\": default must be a number");
      }
      String lit = node.getAsString();
      boolean hasFraction = lit.indexOf('.') >= 0 || lit.indexOf('e') >= 0 || lit.indexOf('E') >= 0;
      return DefaultValue.Num.of(hasFraction ? lit : lit + ".0");
    }
    if (type == PropType.ScalarType.BOOLEAN) {
      if (!isBoolean(node)) {
        throw fail(file, "property \"" + prop + "\": default must be a boolean");
      }
      return DefaultValue.Bool.of(node.getAsBoolean());
    }
    if (type == PropType.ScalarType.BYTES) {
      throw fail(file, "property \"" + prop + "\": default is not supported for \"bytes\"");
    }
    if (type instanceof PropType.ArrayType array) {
      if (!node.isJsonArray()) {
        throw fail(file, "property \"" + prop + "\": default must be an array");
      }
      if (!(array.getItems() instanceof PropType.ScalarType)) {
        throw fail(
            file, "property \"" + prop + "\": array default is only supported for scalar items");
      }
      JsonArray elementsNode = node.getAsJsonArray();
      List<DefaultValue> elements = new ArrayList<>();
      for (int i = 0; i < elementsNode.size(); i++) {
        elements.add(toDefaultValue(file, prop + "[]", elementsNode.get(i), array.getItems()));
      }
      return DefaultValue.Arr.of(List.copyOf(elements));
    }
    throw fail(
        file, "property \"" + prop + "\": default is not supported for object / $ref properties");
  }

  /**
   * Constraints for one property. An {@code array} node contributes only {@code minItems} / {@code
   * maxItems} (as {@code @Size} on the {@code List}); the facets inside its {@code items} schema
   * become {@link Constraints#getElementConstraints()}, rendered as container-element annotations
   * on the field.
   *
   * @param node the property node.
   * @param type the property's resolved type.
   * @return the constraint set.
   */
  private static Constraints parseConstraints(JsonObject node, PropType type) {
    if (type instanceof PropType.ArrayType array) {
      JsonElement itemsNode = get(node, "items");
      Constraints elementConstraints =
          itemsNode != null && itemsNode.isJsonObject()
              ? nonEmptyOrNull(parseValueConstraints(itemsNode.getAsJsonObject(), array.getItems()))
              : null;
      return Constraints.builder()
          .minItems(intOrNull(node, "minItems"))
          .maxItems(intOrNull(node, "maxItems"))
          .elementConstraints(elementConstraints)
          .build();
    }
    return parseValueConstraints(node, type);
  }

  private static @Nullable Constraints nonEmptyOrNull(Constraints constraints) {
    return constraints.equals(Constraints.none()) ? null : constraints;
  }

  /**
   * The {@code string} / {@code integer} / {@code number} value facets ({@code pattern}, {@code
   * enum}, {@code format}, {@code minLength} / {@code maxLength}, {@code minimum} / {@code
   * maximum}) of a scalar schema node - a property's own, or an {@code array}'s {@code items}.
   *
   * @param node the schema node carrying the facets.
   * @param type the resolved scalar type of that node.
   * @return the constraint set ({@link Constraints#none()} when no facet is present).
   */
  private static Constraints parseValueConstraints(JsonObject node, PropType type) {
    boolean isString = type == PropType.ScalarType.STRING;
    boolean isInteger = type == PropType.ScalarType.INTEGER || type == PropType.ScalarType.LONG;
    boolean isNumber = type == PropType.ScalarType.NUMBER || type == PropType.ScalarType.FLOAT;

    String format = stringOrNull(node, "format");
    String stringFormat = isString ? format : null;
    boolean stringDecimal = "decimal".equals(stringFormat);
    String explicitPattern = stringOrNull(node, "pattern");
    Format formatPattern = stringFormat != null ? STRING_FORMATS.get(stringFormat) : null;
    Format enumPattern = isString ? enumFormat(node) : null;

    return Constraints.builder()
        .pattern(
            explicitPattern != null
                ? explicitPattern
                : enumPattern != null ? enumPattern.pattern() : mapPattern(formatPattern))
        .patternMessage(
            explicitPattern != null
                ? null
                : enumPattern != null ? enumPattern.message() : mapMessage(formatPattern))
        .minLength(intOrNull(node, "minLength"))
        .maxLength(intOrNull(node, "maxLength"))
        .email("email".equals(format))
        .minimum(isInteger ? longOrNull(node, "minimum") : null)
        .maximum(isInteger ? longOrNull(node, "maximum") : null)
        .decimalMinimum(isNumber || stringDecimal ? numberStringOrNull(node, "minimum") : null)
        .decimalMaximum(isNumber || stringDecimal ? numberStringOrNull(node, "maximum") : null)
        .build();
  }

  /**
   * {@code "enum": [...]}, a fixed set of allowed string values - rendered as {@code @Pattern}
   * alternation, never a Java {@code enum}. {@code null} when {@code "enum"} is absent or empty.
   *
   * @param node the property node.
   * @return the pattern and message pair, or {@code null}.
   */
  private static @Nullable Format enumFormat(JsonObject node) {
    JsonElement enumNode = get(node, "enum");
    if (enumNode == null || !enumNode.isJsonArray()) {
      return null;
    }
    List<String> values = new ArrayList<>();
    for (JsonElement e : enumNode.getAsJsonArray()) {
      if (isString(e)) {
        values.add(e.getAsString());
      }
    }
    if (values.isEmpty()) {
      return null;
    }
    String pattern = "^(" + values.stream().map(Pattern::quote).collect(joining("|")) + ")$";
    String message = "must be one of " + String.join(", ", values);
    return new Format(pattern, message);
  }

  private static @Nullable String mapPattern(@Nullable Format f) {
    return f == null ? null : f.pattern();
  }

  private static @Nullable String mapMessage(@Nullable Format f) {
    return f == null ? null : f.message();
  }

  /**
   * Checks whether a {@code $ref} targets a built-in JDK type.
   *
   * @param ref the {@code $ref} value.
   * @return {@code true} for a {@code java.*} / {@code javax.*} reference.
   */
  private static boolean isBuiltInJavaType(String ref) {
    return ref.startsWith("java.") || ref.startsWith("javax.");
  }

  /**
   * Requires a property key to be camelCase, snake_case, or UpperCamelCase, optionally prefixed
   * with one or more underscores (e.g. {@code __metadata}); returns its camelCase form, with the
   * underscore prefix preserved.
   *
   * @param file the schema file being parsed, for error messages.
   * @param typeName the enclosing type's name, for error messages.
   * @param key the raw JSON property key.
   * @return the key's camelCase identifier form.
   */
  private static String toIdentifier(Path file, String typeName, String key) {
    Matcher m = LEADING_UNDERSCORES.matcher(key);
    String prefix = m.find() ? m.group() : "";
    String body = key.substring(prefix.length());
    if (body.isEmpty()
        || (!CAMEL_CASE.matcher(body).matches()
            && !SNAKE_CASE.matcher(body).matches()
            && !UPPER_CAMEL_CASE.matcher(body).matches())) {
      throw fail(
          file,
          "type \""
              + typeName
              + "\": property \""
              + key
              + "\" must be camelCase, snake_case, or UpperCamelCase, optionally prefixed with"
              + " underscores");
    }
    String[] parts = body.split("_", -1);
    StringBuilder joined = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      joined.append(i == 0 ? parts[i] : capitalize(parts[i]));
    }
    String identifier = uncapitalize(joined.toString());
    return prefix + identifier;
  }

  private static IllegalStateException fail(Path file, String message) {
    return new IllegalStateException(fileName(file) + ": " + message);
  }

  /**
   * @param file the file to name.
   * @return {@code file}'s file name component, as a string.
   */
  private static String fileName(Path file) {
    return file.getFileName().toString();
  }

  /**
   * @param node the JSON object to read from.
   * @param field the field name.
   * @return the field value, or {@code null} when the field is absent or JSON {@code null}.
   */
  private static @Nullable JsonElement get(JsonObject node, String field) {
    JsonElement value = node.get(field);
    return value == null || value.isJsonNull() ? null : value;
  }

  private static boolean isString(JsonElement node) {
    return node.isJsonPrimitive() && node.getAsJsonPrimitive().isString();
  }

  private static boolean isBoolean(@Nullable JsonElement node) {
    return node != null && node.isJsonPrimitive() && node.getAsJsonPrimitive().isBoolean();
  }

  private static boolean isNumber(JsonElement node) {
    return node.isJsonPrimitive() && node.getAsJsonPrimitive().isNumber();
  }

  /**
   * @param node the JSON node to check.
   * @return {@code true} for a number literal with no fractional or exponent part, e.g. {@code 5}
   *     but not {@code 5.0}.
   */
  private static boolean isIntegralNumber(@Nullable JsonElement node) {
    if (node == null || !isNumber(node)) {
      return false;
    }
    String lit = node.getAsString();
    return lit.indexOf('.') < 0 && lit.indexOf('e') < 0 && lit.indexOf('E') < 0;
  }

  private static @Nullable String stringOrNull(JsonObject node, String field) {
    JsonElement n = get(node, field);
    return n != null && isString(n) ? n.getAsString() : null;
  }

  /**
   * Reads a property's {@code "example"} as a string, for {@code @Schema(example = "...")}. A
   * scalar ({@code string}, number or boolean) is taken verbatim; an object / array example is
   * ignored.
   *
   * @param node the property node.
   * @return the example rendered as a string, or {@code null} when absent or not a scalar.
   */
  private static @Nullable String exampleStringOrNull(JsonObject node) {
    JsonElement n = get(node, "example");
    if (n == null || !n.isJsonPrimitive()) {
      return null;
    }
    return n.getAsString();
  }

  private static @Nullable Integer intOrNull(JsonObject node, String field) {
    JsonElement n = get(node, field);
    return n != null && isNumber(n) ? n.getAsInt() : null;
  }

  private static @Nullable Long longOrNull(JsonObject node, String field) {
    JsonElement n = get(node, field);
    return n != null && isNumber(n) ? n.getAsLong() : null;
  }

  private static @Nullable String numberStringOrNull(JsonObject node, String field) {
    JsonElement n = get(node, field);
    return n != null && isNumber(n) ? n.getAsString() : null;
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static String uncapitalize(String s) {
    return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }
}
