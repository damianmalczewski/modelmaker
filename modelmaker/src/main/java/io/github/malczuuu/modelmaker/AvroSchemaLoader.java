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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * {@link SchemaLoader} that parses Apache Avro {@code .avsc} record schemas.
 *
 * <p><b>Experimental.</b> Avro input is not covered by this project's compatibility promise: this
 * class, and support for {@code .avsc} files in general, may change or be removed in any release,
 * including a minor one. Code against {@link SimpleSchemaLoader} (or {@link
 * SchemaLoaders#createDelegatingSchemaLoader()}) for anything that must keep working.
 *
 * <p>Hand-parsed as plain JSON, the same way {@link SimpleSchemaLoader} parses its own format,
 * rather than depending on {@code org.apache.avro:avro} for this project's intentionally narrow
 * schema subset.
 *
 * <p>Not supported (fails fast with a clear message): {@code map}, {@code fixed}, a union with
 * anything other than exactly one non-null branch, and Avro IDL ({@code .avdl}) files. A {@code
 * logicalType} is ignored - the field maps to its base Avro type.
 */
public final class AvroSchemaLoader implements SchemaLoader {

  /** Creates a new {@link AvroSchemaLoader}. */
  public AvroSchemaLoader() {}

  /**
   * Accepts only {@code "avsc"} files.
   *
   * @param extension a file extension, case-insensitively compared.
   * @return {@code true} for {@code "avsc"}.
   */
  @Override
  public boolean canLoad(String extension) {
    return "avsc".equalsIgnoreCase(extension);
  }

  /**
   * Parses each file independently, then validates every named-type reference across all of them.
   *
   * @param files schema files to parse; processing order is by file name, independent of the list
   *     order.
   * @return one {@link ModelType} per file, with all named-type references resolved and validated.
   */
  @Override
  public List<ModelType> load(List<Path> files) {
    Objects.requireNonNull(files, "files must not be null");

    List<Path> sorted = new ArrayList<>(files);
    sorted.sort(Comparator.comparing(AvroSchemaLoader::fileName));
    List<ModelType> types = new ArrayList<>();
    List<SchemaException> errors = new ArrayList<>();
    for (Path f : sorted) {
      try {
        types.add(parseFile(f));
      } catch (SchemaException e) {
        // Keep going: one run should report every broken schema, not just the first one.
        errors.add(e);
      }
    }
    if (!errors.isEmpty()) {
      throw SchemaException.of(errors);
    }
    SchemaReferences.validate(types);
    return List.copyOf(types);
  }

  private static ModelType parseFile(Path file) {
    JsonElement parsed;
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      parsed = JsonParser.parseReader(reader);
    } catch (Exception e) {
      throw new SchemaException(
          fileName(file), fileName(file) + ": not valid JSON (" + e.getMessage() + ")", e);
    }
    if (!parsed.isJsonObject()) {
      throw fail(file, "root must be a JSON object");
    }
    JsonObject root = parsed.getAsJsonObject();
    if (!"record".equals(stringOrNull(root, "type"))) {
      throw fail(file, "top-level \"type\" must be \"record\"");
    }
    String namespace = requireString(file, root, "namespace", "top-level record");
    String name = requireString(file, root, "name", "top-level record");
    String expectedFileName = namespace + "." + name + ".avsc";
    if (!fileName(file).equals(expectedFileName)) {
      throw fail(
          file, "file must be named \"" + expectedFileName + "\" to match its namespace and name");
    }
    return parseRecord(file, root, name, namespace, parseFeatureOverrides(file, root));
  }

  /**
   * Parses one Avro {@code record} - the top-level record, or an inline nested one - into a {@link
   * ModelType}. Nested records carry the same {@code packageName} as their enclosing type, just
   * like the JSON format's inline {@code object}s, and always get {@link FeatureOverrides#none()} -
   * only the top-level record's own {@code "features"} applies.
   */
  private static ModelType parseRecord(
      Path file,
      JsonObject node,
      String name,
      String packageName,
      FeatureOverrides featureOverrides) {
    JsonElement fieldsNode = get(node, "fields");
    if (fieldsNode == null || !fieldsNode.isJsonArray()) {
      throw fail(file, "type \"" + name + "\": missing \"fields\" array");
    }

    List<Property> properties = new ArrayList<>();
    List<ModelType> nested = new ArrayList<>();
    Set<String> fieldNames = new HashSet<>();
    for (JsonElement fieldElement : fieldsNode.getAsJsonArray()) {
      if (!fieldElement.isJsonObject()) {
        throw fail(file, "type \"" + name + "\": each field must be an object");
      }
      Property property =
          parseField(file, name, packageName, fieldElement.getAsJsonObject(), nested);
      if (!fieldNames.add(property.getName())) {
        throw fail(
            file, "type \"" + name + "\": field \"" + property.getName() + "\" is declared twice");
      }
      properties.add(property);
    }

    String doc = stringOrNull(node, "doc");
    return new ModelType(
        name, packageName, doc, List.copyOf(properties), List.copyOf(nested), featureOverrides);
  }

  /**
   * Reads a top-level record's {@code "features"} object, a per-schema override of {@code
   * modelmaker { features { } }} - same keys, same semantics, as the Simple Schema format's
   * top-level {@code "features"}.
   *
   * @param file the schema file being parsed, for error messages.
   * @param root the top-level record node.
   * @return the parsed overrides, {@link FeatureOverrides#none()} when {@code "features"} is
   *     absent.
   */
  private static FeatureOverrides parseFeatureOverrides(Path file, JsonObject root) {
    JsonElement featuresNode = get(root, "features");
    if (featuresNode != null && !featuresNode.isJsonObject()) {
      throw fail(file, "\"features\" must be an object");
    }
    JsonObject features = featuresNode != null ? featuresNode.getAsJsonObject() : null;
    return new FeatureOverridesParser(message -> fail(file, message)).parse(features);
  }

  private static Property parseField(
      Path file,
      String typeName,
      String packageName,
      JsonObject field,
      List<ModelType> nestedSink) {
    String fieldName = requireString(file, field, "name", "type \"" + typeName + "\": field");
    JsonElement typeNode = get(field, "type");
    if (typeNode == null) {
      throw fail(file, "field \"" + fieldName + "\": missing \"type\"");
    }

    boolean nullable = false;
    JsonElement effective = typeNode;
    if (typeNode.isJsonArray()) {
      List<JsonElement> nonNullBranches = new ArrayList<>();
      for (JsonElement branch : typeNode.getAsJsonArray()) {
        if (isString(branch) && "null".equals(branch.getAsString())) {
          nullable = true;
        } else {
          nonNullBranches.add(branch);
        }
      }
      if (!nullable || nonNullBranches.size() != 1) {
        throw fail(
            file,
            "field \""
                + fieldName
                + "\": unsupported union (only [\"null\", T] - a nullable single type - is"
                + " supported)");
      }
      effective = nonNullBranches.get(0);
    }

    PropType type = resolveType(file, fieldName, packageName, effective, nestedSink);
    Constraints constraints = Constraints.none();
    if (effective.isJsonObject()
        && "enum".equals(stringOrNull(effective.getAsJsonObject(), "type"))) {
      constraints = enumConstraints(file, fieldName, effective.getAsJsonObject());
    }

    boolean required = !nullable;
    DefaultValue defaultValue =
        required ? null : parseDefault(file, fieldName, get(field, "default"), type);

    return new Property(
        fieldName,
        type,
        required,
        constraints,
        fieldName,
        defaultValue,
        stringOrNull(field, "doc"),
        null);
  }

  /**
   * Resolves one Avro type node - already past any nullable-union unwrapping - to a {@link
   * PropType}. Recurses for {@code array} items; a nested {@code record} is appended to {@code
   * nestedSink} and referenced back by name, the same as the JSON format's inline {@code object}.
   */
  private static PropType resolveType(
      Path file,
      String nameHint,
      String packageName,
      JsonElement typeNode,
      List<ModelType> nestedSink) {
    if (isString(typeNode)) {
      return scalarOrRef(typeNode.getAsString());
    }
    if (!typeNode.isJsonObject()) {
      throw fail(file, "field \"" + nameHint + "\": unsupported type " + typeNode);
    }
    JsonObject obj = typeNode.getAsJsonObject();
    String kind = stringOrNull(obj, "type");
    if (kind == null) {
      throw fail(file, "field \"" + nameHint + "\": needs \"type\"");
    }
    return switch (kind) {
      case "record" -> {
        String nestedName = requireString(file, obj, "name", "field \"" + nameHint + "\"");
        nestedSink.add(parseRecord(file, obj, nestedName, packageName, FeatureOverrides.none()));
        yield PropType.RefType.of(nestedName);
      }
      case "enum" ->
          // The symbols become an @Pattern alternation, built by the caller from the same node -
          // never a Java enum.
          PropType.ScalarType.STRING;
      case "array" -> {
        JsonElement items = get(obj, "items");
        if (items == null) {
          throw fail(file, "field \"" + nameHint + "\": array needs \"items\"");
        }
        PropType itemType = resolveType(file, nameHint, packageName, items, nestedSink);
        if (itemType instanceof PropType.ArrayType) {
          throw fail(file, "field \"" + nameHint + "\": nested arrays are not supported");
        }
        yield PropType.ArrayType.of(itemType);
      }
      case "map" -> throw fail(file, "field \"" + nameHint + "\": \"map\" is not supported");
      case "fixed" -> throw fail(file, "field \"" + nameHint + "\": \"fixed\" is not supported");
      // e.g. {"type": "long", "logicalType": "timestamp-millis"} - logicalType is ignored, the
      // field maps to its base Avro type ("long", here).
      default -> scalarOrRef(kind);
    };
  }

  private static PropType scalarOrRef(String t) {
    return switch (t) {
      case "string" -> PropType.ScalarType.STRING;
      case "bytes" -> PropType.ScalarType.BYTES;
      case "int" -> PropType.ScalarType.INTEGER;
      case "long" -> PropType.ScalarType.LONG;
      case "float" -> PropType.ScalarType.FLOAT;
      case "double" -> PropType.ScalarType.NUMBER;
      case "boolean" -> PropType.ScalarType.BOOLEAN;
      default -> PropType.RefType.of(t);
    };
  }

  /**
   * {@code {"type": "enum", "symbols": [...]}} - rendered as an {@code @Pattern} alternation over
   * the symbols, never a Java {@code enum}.
   */
  private static Constraints enumConstraints(Path file, String fieldName, JsonObject enumNode) {
    JsonElement symbolsNode = get(enumNode, "symbols");
    if (symbolsNode == null || !symbolsNode.isJsonArray()) {
      throw fail(file, "field \"" + fieldName + "\": enum needs \"symbols\"");
    }
    List<String> symbols = new ArrayList<>();
    for (JsonElement s : symbolsNode.getAsJsonArray()) {
      if (isString(s)) {
        symbols.add(s.getAsString());
      }
    }
    String pattern = "^(" + symbols.stream().map(Pattern::quote).collect(joining("|")) + ")$";
    String message = "must be one of " + String.join(", ", symbols);
    return Constraints.builder().pattern(pattern).patternMessage(message).build();
  }

  private static @Nullable DefaultValue parseDefault(
      Path file, String fieldName, @Nullable JsonElement node, PropType type) {
    if (node == null || node.isJsonNull()) {
      return null;
    }
    if (type == PropType.ScalarType.STRING) {
      if (!isString(node)) {
        throw fail(file, "field \"" + fieldName + "\": default must be a string");
      }
      return DefaultValue.Str.of(node.getAsString());
    }
    if (type == PropType.ScalarType.INTEGER || type == PropType.ScalarType.LONG) {
      if (!isIntegralNumber(node)) {
        throw fail(file, "field \"" + fieldName + "\": default must be an integer");
      }
      return DefaultValue.Num.of(Long.toString(node.getAsLong()));
    }
    if (type == PropType.ScalarType.NUMBER || type == PropType.ScalarType.FLOAT) {
      if (!isNumber(node)) {
        throw fail(file, "field \"" + fieldName + "\": default must be a number");
      }
      String lit = node.getAsString();
      boolean hasFraction = lit.indexOf('.') >= 0 || lit.indexOf('e') >= 0 || lit.indexOf('E') >= 0;
      return DefaultValue.Num.of(hasFraction ? lit : lit + ".0");
    }
    if (type == PropType.ScalarType.BOOLEAN) {
      if (!isBoolean(node)) {
        throw fail(file, "field \"" + fieldName + "\": default must be a boolean");
      }
      return DefaultValue.Bool.of(node.getAsBoolean());
    }
    if (type == PropType.ScalarType.BYTES) {
      throw fail(file, "field \"" + fieldName + "\": default is not supported for \"bytes\"");
    }
    if (type instanceof PropType.ArrayType array) {
      if (!node.isJsonArray()) {
        throw fail(file, "field \"" + fieldName + "\": default must be an array");
      }
      if (!(array.getItems() instanceof PropType.ScalarType)) {
        throw fail(
            file, "field \"" + fieldName + "\": array default is only supported for scalar items");
      }
      JsonArray elementsNode = node.getAsJsonArray();
      List<DefaultValue> elements = new ArrayList<>();
      for (int i = 0; i < elementsNode.size(); i++) {
        elements.add(parseDefaultElement(file, fieldName, elementsNode.get(i), array.getItems()));
      }
      return DefaultValue.Arr.of(List.copyOf(elements));
    }
    throw fail(file, "field \"" + fieldName + "\": default is not supported for this type");
  }

  private static DefaultValue parseDefaultElement(
      Path file, String fieldName, JsonElement node, PropType itemType) {
    DefaultValue value = parseDefault(file, fieldName + "[]", node, itemType);
    if (value == null) {
      throw fail(file, "field \"" + fieldName + "\": array default elements must not be null");
    }
    return value;
  }

  private static String requireString(Path file, JsonObject node, String field, String context) {
    String value = stringOrNull(node, field);
    if (value == null) {
      throw fail(file, context + ": missing \"" + field + "\"");
    }
    return value;
  }

  private static SchemaException fail(Path file, String message) {
    return new SchemaException(fileName(file), fileName(file) + ": " + message, null);
  }

  private static String fileName(Path file) {
    return file.getFileName().toString();
  }

  private static @Nullable JsonElement get(JsonObject node, String field) {
    JsonElement value = node.get(field);
    return value == null || value.isJsonNull() ? null : value;
  }

  private static boolean isString(JsonElement node) {
    return node.isJsonPrimitive() && node.getAsJsonPrimitive().isString();
  }

  private static boolean isBoolean(JsonElement node) {
    return node.isJsonPrimitive() && node.getAsJsonPrimitive().isBoolean();
  }

  private static boolean isNumber(JsonElement node) {
    return node.isJsonPrimitive() && node.getAsJsonPrimitive().isNumber();
  }

  private static boolean isIntegralNumber(JsonElement node) {
    if (!isNumber(node)) {
      return false;
    }
    String lit = node.getAsString();
    return lit.indexOf('.') < 0 && lit.indexOf('e') < 0 && lit.indexOf('E') < 0;
  }

  private static @Nullable String stringOrNull(JsonObject node, String field) {
    JsonElement n = get(node, field);
    return n != null && isString(n) ? n.getAsString() : null;
  }
}
