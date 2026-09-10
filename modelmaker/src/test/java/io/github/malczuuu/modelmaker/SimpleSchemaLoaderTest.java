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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleSchemaLoaderTest {

  private final SchemaLoader loader = new SimpleSchemaLoader();

  @TempDir Path dir;

  /**
   * Writes {@code <title>.json} so the file name matches the schema title, as the loader requires.
   */
  private Path schema(String title, String body) {
    return write(
        title + ".json",
        "{ \"$modelmaker\": \"v1.0\", \"title\": \""
            + title
            + "\", \"type\": \"object\", "
            + body
            + " }");
  }

  private Path write(String name, String content) {
    Path f = dir.resolve(name);
    try {
      Files.writeString(f, content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return f;
  }

  private static ModelType single(List<ModelType> types, Predicate<ModelType> predicate) {
    return types.stream().filter(predicate).findFirst().orElseThrow();
  }

  private static ModelType named(List<ModelType> types, String name) {
    return single(types, t -> t.getName().equals(name));
  }

  private static Property prop(ModelType type, String name) {
    return type.getProperties().stream()
        .filter(p -> p.getName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static List<String> propNames(ModelType type) {
    return type.getProperties().stream().map(Property::getName).toList();
  }

  @Test
  void parsesScalarsRefsArraysAndRequired() {
    schema(
        "com.example.dto.Address",
        "\"required\": [\"street\"], \"properties\": { \"street\": { \"type\": \"string\" } }");
    Path person =
        schema(
            "com.example.dto.Person",
            """
            "required": ["id", "home"],
            "properties": {
              "id":    { "type": "string", "pattern": "^X" },
              "age":   { "type": "integer", "minimum": 0 },
              "home":  { "$ref": "com.example.dto.Address" },
              "tags":  { "type": "array", "items": { "type": "string" }, "maxItems": 3 }
            }
            """);

    List<ModelType> types =
        loader.load(List.of(person, dir.resolve("com.example.dto.Address.json")));
    ModelType p = single(types, t -> t.getName().equals("Person"));

    assertThat(p.getName()).isEqualTo("Person");
    assertThat(p.getPackageName()).isEqualTo("com.example.dto");
    assertThat(propNames(p)).containsExactly("id", "age", "home", "tags");
    assertThat(prop(p, "home").getType()).isEqualTo(PropType.RefType.of("com.example.dto.Address"));
    assertThat(prop(p, "tags").getType())
        .isEqualTo(PropType.ArrayType.of(PropType.ScalarType.STRING));
  }

  @Test
  void readsElementConstraintsFromItemsAndCollectionConstraintsFromTheArrayNode() {
    Path f =
        schema(
            "x.DeviceRequest",
            "\"properties\": { \"tags\": { \"type\": \"array\", \"minItems\": 1, \"maxItems\": 8,"
                + " \"items\": { \"type\": \"string\", \"minLength\": 1, \"maxLength\": 255,"
                + " \"pattern\": \"^[a-z]+$\" } } }");

    Constraints c = prop(loader.load(List.of(f)).get(0), "tags").getConstraints();

    assertThat(c.getMinItems()).isEqualTo(1);
    assertThat(c.getMaxItems()).isEqualTo(8);
    Constraints element = c.getElementConstraints();
    assertThat(element).isNotNull();
    assertThat(element.getMinLength()).isEqualTo(1);
    assertThat(element.getMaxLength()).isEqualTo(255);
    assertThat(element.getPattern()).isEqualTo("^[a-z]+$");
  }

  @Test
  void scalarFacetsOnTheArrayNodeItselfAreIgnored() {
    Path f =
        schema(
            "x.Bag",
            "\"properties\": { \"tags\": { \"type\": \"array\", \"minLength\": 2, \"pattern\":"
                + " \"^x$\", \"items\": { \"type\": \"string\" } } }");

    Constraints c = prop(loader.load(List.of(f)).get(0), "tags").getConstraints();

    assertThat(c.getMinLength()).isNull();
    assertThat(c.getPattern()).isNull();
    assertThat(c.getElementConstraints()).isNull();
  }

  @Test
  void anArrayWithNoItemFacetsHasNoElementConstraints() {
    Path f =
        schema(
            "x.Bag",
            "\"properties\": { \"tags\": { \"type\": \"array\", \"maxItems\": 3, \"items\": {"
                + " \"type\": \"string\" } } }");

    Constraints c = prop(loader.load(List.of(f)).get(0), "tags").getConstraints();

    assertThat(c.getMaxItems()).isEqualTo(3);
    assertThat(c.getElementConstraints()).isNull();
  }

  @Test
  void parsesAnInlineObjectAsANestedTypePlainTitleAllowed() {
    Path f =
        schema(
            "com.example.dto.Order",
            """
            "required": ["shipTo"],
            "properties": {
              "shipTo": {
                "type": "object", "title": "ShipAddress",
                "properties": { "city": { "type": "string" } }
              }
            }
            """);

    ModelType order = loader.load(List.of(f)).get(0);

    assertThat(order.getNested().stream().map(ModelType::getName)).containsExactly("ShipAddress");
    assertThat(order.getProperties().get(0).getType())
        .isEqualTo(PropType.RefType.of("ShipAddress"));
  }

  @Test
  void nestedTitleDerivingFromASnakeCaseKey() {
    Path f =
        schema(
            "com.example.dto.U",
            "\"properties\": { \"ship_to\": { \"type\": \"object\", \"properties\": { \"a\": {"
                + " \"type\": \"string\" } } } }");
    assertThat(loader.load(List.of(f)).get(0).getNested().get(0).getName()).isEqualTo("ShipTo");
  }

  @Test
  void formatDecimalAddsAPatternAndDecimalMinMaxOnAString() {
    Path f =
        schema(
            "x.Money",
            """
            "properties": {
              "auto":  { "type": "string", "format": "decimal", "minimum": 0, "maximum": 100 },
              "plain": { "type": "string", "minimum": 5 }
            }
            """);
    ModelType money = loader.load(List.of(f)).get(0);
    Constraints auto = prop(money, "auto").getConstraints();
    assertThat(auto.getPattern()).isEqualTo("^-?\\d+(\\.\\d+)?$");
    assertThat(auto.getDecimalMinimum()).isEqualTo("0");
    assertThat(prop(money, "plain").getConstraints().getDecimalMinimum()).isNull();
  }

  @Test
  void stringFormatsMapToAPatternWithAFormatSpecificMessage() {
    Path f =
        schema(
            "x.Formats",
            """
            "properties": {
              "d": { "type": "string", "format": "date" },
              "u": { "type": "string", "format": "uuid" }
            }
            """);
    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(prop(m, "d").getConstraints().getPattern()).isEqualTo("^\\d{4}-\\d{2}-\\d{2}$");
    assertThat(prop(m, "d").getConstraints().getPatternMessage())
        .isEqualTo("must be a date in yyyy-MM-dd format");
    assertThat(prop(m, "u").getConstraints().getPatternMessage()).isEqualTo("must be a UUID");
  }

  @Test
  void snakeCaseAndUpperCamelCaseKeysBecomeCamelCaseIdentifiersJsonNameKeepsTheKey() {
    Path f =
        schema(
            "x.U",
            "\"properties\": { \"user_id\": { \"type\": \"string\" }, \"lastLoginAt\": { \"type\":"
                + " \"string\" }, \"AccountType\": { \"type\": \"string\" } }");
    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(propNames(m)).containsExactlyInAnyOrder("userId", "lastLoginAt", "accountType");
    assertThat(prop(m, "userId").getJsonName()).isEqualTo("user_id");
    assertThat(prop(m, "lastLoginAt").getJsonName()).isEqualTo("lastLoginAt");
    assertThat(prop(m, "accountType").getJsonName()).isEqualTo("AccountType");
  }

  @Test
  void leadingUnderscoresArePreservedOnAnOtherwiseValidKey() {
    Path f =
        schema(
            "x.U",
            "\"properties\": { \"__metadata\": { \"type\": \"string\" }, \"_userId\": { \"type\":"
                + " \"string\" }, \"__account_type\": { \"type\": \"string\" } }");
    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(propNames(m)).containsExactlyInAnyOrder("__metadata", "_userId", "__accountType");
    assertThat(prop(m, "__metadata").getJsonName()).isEqualTo("__metadata");
    assertThat(prop(m, "_userId").getJsonName()).isEqualTo("_userId");
    assertThat(prop(m, "__accountType").getJsonName()).isEqualTo("__account_type");
  }

  @Test
  void parsesDefaultValuesAndMarksThePropertyNonNull() {
    Path f =
        schema(
            "x.D",
            """
            "properties": {
              "active":   { "type": "boolean", "default": true },
              "currency": { "type": "string", "default": "USD" },
              "ratio":    { "type": "number", "default": 1 },
              "roles":    { "type": "array", "items": { "type": "string" }, "default": ["user"] }
            }
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "active").getDefaultValue()).isEqualTo(DefaultValue.Bool.of(true));
    assertThat(prop(m, "currency").getDefaultValue()).isEqualTo(DefaultValue.Str.of("USD"));
    assertThat(prop(m, "ratio").getDefaultValue()).isEqualTo(DefaultValue.Num.of("1.0"));
    assertThat(prop(m, "roles").getDefaultValue())
        .isEqualTo(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("user"))));
    m.getProperties().forEach(it -> assertThat(it.nonNull()).isTrue());
  }

  @Test
  void rejectsADefaultOfTheWrongType() {
    Path f =
        schema("x.D", "\"properties\": { \"n\": { \"type\": \"integer\", \"default\": \"x\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be an integer");
  }

  @Test
  void rejectsADefaultOnARefProperty() {
    schema("x.A", "\"properties\": { \"s\": { \"type\": \"string\" } }");
    Path f = schema("x.B", "\"properties\": { \"a\": { \"$ref\": \"x.A\", \"default\": {} } }");
    assertThatThrownBy(() -> loader.load(List.of(f, dir.resolve("x.A.json"))))
        .hasMessageContaining("default is not supported for object");
  }

  @Test
  void parsesABytesProperty() {
    Path f =
        schema(
            "x.Blob",
            "\"required\": [\"payload\"], \"properties\": { \"payload\": { \"type\": \"bytes\" },"
                + " \"signature\": { \"type\": \"bytes\" } }");
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "payload").getType()).isEqualTo(PropType.ScalarType.BYTES);
    assertThat(prop(m, "payload").isRequired()).isTrue();
    assertThat(prop(m, "signature").getType()).isEqualTo(PropType.ScalarType.BYTES);
    assertThat(prop(m, "signature").isRequired()).isFalse();
  }

  @Test
  void rejectsTwoPropertiesThatMapToTheSameFieldName() {
    Path f =
        schema(
            "x.Person",
            "\"properties\": { \"first_name\": { \"type\": \"string\" },"
                + " \"firstName\": { \"type\": \"string\" } }");

    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"first_name\" and \"firstName\" both map to the field name")
        .hasMessageContaining("\"firstName\"");
  }

  @Test
  void rejectsADefaultOnABytesProperty() {
    Path f =
        schema("x.D", "\"properties\": { \"b\": { \"type\": \"bytes\", \"default\": \"AA==\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default is not supported for \"bytes\"");
  }

  @Test
  void parsesAValidIntegerDefault() {
    Path f = schema("x.D", "\"properties\": { \"n\": { \"type\": \"integer\", \"default\": 42 } }");
    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(prop(m, "n").getDefaultValue()).isEqualTo(DefaultValue.Num.of("42"));
  }

  @Test
  void rejectsAStringDefaultOfTheWrongType() {
    Path f = schema("x.D", "\"properties\": { \"s\": { \"type\": \"string\", \"default\": 5 } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be a string");
  }

  @Test
  void rejectsANumberDefaultOfTheWrongType() {
    Path f =
        schema("x.D", "\"properties\": { \"r\": { \"type\": \"number\", \"default\": \"x\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be a number");
  }

  @Test
  void rejectsABooleanDefaultOfTheWrongType() {
    Path f =
        schema("x.D", "\"properties\": { \"b\": { \"type\": \"boolean\", \"default\": \"x\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be a boolean");
  }

  @Test
  void rejectsAnArrayDefaultThatIsNotAnArray() {
    Path f =
        schema(
            "x.D",
            "\"properties\": { \"a\": { \"type\": \"array\", \"items\": { \"type\": \"string\" },"
                + " \"default\": \"x\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be an array");
  }

  @Test
  void rejectsAnArrayDefaultWithNonScalarItems() {
    Path f =
        schema(
            "x.D",
            "\"properties\": { \"a\": { \"type\": \"array\", \"items\": { \"type\": \"object\","
                + " \"properties\": {} }, \"default\": [] } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("array default is only supported for scalar items");
  }

  @Test
  void rejectsInvalidJson() {
    Path f = write("x.Bad.json", "{ not json");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("not valid JSON");
  }

  @Test
  void rejectsATopLevelTypeThatIsNotObject() {
    Path f =
        write(
            "x.Bad.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Bad\", \"type\": \"array\","
                + " \"properties\": {} }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("top-level \"type\" must be \"object\"");
  }

  @Test
  void rejectsAMissingPropertiesObject() {
    Path f = schema("x.Bad", "\"required\": []");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("missing \"properties\" object");
  }

  @Test
  void rejectsAPropertyMissingTypeAndRef() {
    Path f = schema("x.Bad", "\"properties\": { \"a\": {} }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("needs \"type\" or \"$ref\"");
  }

  @Test
  void rejectsAnArrayMissingItems() {
    Path f = schema("x.Bad", "\"properties\": { \"a\": { \"type\": \"array\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("array needs \"items\"");
  }

  @Test
  void rejectsNestedArrays() {
    Path f =
        schema(
            "x.Bad",
            "\"properties\": { \"a\": { \"type\": \"array\", \"items\": { \"type\": \"array\","
                + " \"items\": { \"type\": \"string\" } } } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("nested arrays are not supported");
  }

  @Test
  void rejectsAPropertyKeyThatIsNeitherCamelCaseSnakeCaseNorUpperCamelCase() {
    for (String key : List.of("user-id", "user__id", "1st", "_", "___")) {
      Path f = schema("x.Bad", "\"properties\": { \"" + key + "\": { \"type\": \"string\" } }");
      assertThatThrownBy(() -> loader.load(List.of(f)))
          .hasMessageContaining("must be camelCase, snake_case, or UpperCamelCase");
    }
  }

  @Test
  void rejectsAnUnknownType() {
    Path f = schema("x.Bad", "\"properties\": { \"a\": { \"type\": \"date\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unsupported type \"date\"");
  }

  @Test
  void parsesAPerSchemaFeaturesOverride() {
    Path all =
        schema(
            "x.All",
            "\"features\": { \"jackson\": { \"enabled\": true, \"annotateFields\": true,"
                + " \"annotateGetters\": false }, \"validation\": { \"enabled\": false },"
                + " \"withers\": { \"enabled\": true }, \"preferPrimitives\": { \"enabled\": false"
                + " }, \"openApi\": { \"enabled\": true } }, \"properties\": {}");
    Path jacksonOnly =
        schema(
            "x.JacksonOnly",
            "\"features\": { \"jackson\": { \"enabled\": true } }, \"properties\": {}");
    Path none = schema("x.None", "\"properties\": {}");

    List<ModelType> types = loader.load(List.of(all, jacksonOnly, none));

    assertThat(named(types, "All").getFeatureOverrides())
        .isEqualTo(
            FeatureOverrides.builder()
                .jackson(new JacksonOverride(true, true, false))
                .validation(ValidationOverride.enabled(false))
                .withers(true)
                .preferPrimitives(false)
                .openApi(OpenApiOverride.enabled(true))
                .build());
    assertThat(named(types, "JacksonOnly").getFeatureOverrides())
        .isEqualTo(FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build());
    assertThat(named(types, "None").getFeatureOverrides()).isEqualTo(FeatureOverrides.none());
  }

  @Test
  void parsesPerPropertyDescriptionAndExample() {
    Path f =
        schema(
            "x.Account",
            """
            "required": ["id"],
            "properties": {
              "id":     { "type": "string", "description": "the account id", "example": "A-1" },
              "count":  { "type": "integer", "example": 7 },
              "plain":  { "type": "string" }
            }
            """);

    ModelType t = named(loader.load(List.of(f)), "Account");

    assertThat(prop(t, "id").getDescription()).isEqualTo("the account id");
    assertThat(prop(t, "id").getExample()).isEqualTo("A-1");
    assertThat(prop(t, "count").getExample()).isEqualTo("7");
    assertThat(prop(t, "count").getDescription()).isNull();
    assertThat(prop(t, "plain").getDescription()).isNull();
    assertThat(prop(t, "plain").getExample()).isNull();
  }

  @Test
  void rejectsANonBooleanFeaturesFlag() {
    Path f =
        schema(
            "x.Bad", "\"features\": { \"jackson\": { \"enabled\": \"yes\" } }, \"properties\": {}");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"features.jackson.enabled\" must be a boolean");
  }

  @Test
  void rejectsABooleanShorthandFeaturesEntry() {
    Path f = schema("x.Bad", "\"features\": { \"jackson\": true }, \"properties\": {}");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"features.jackson\" must be an object");
  }

  @Test
  void rejectsAnUnknownFeaturesEntry() {
    Path f =
        schema("x.Bad", "\"features\": { \"jacksom\": { \"enabled\": true } }, \"properties\": {}");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unknown \"features\" entry \"jacksom\"");
  }

  @Test
  void rejectsAnUnknownKeyInsideAFeatureEntry() {
    Path f =
        schema(
            "x.Bad",
            "\"features\": { \"withers\": { \"annotateFields\": true } }, \"properties\": {}");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unknown \"features.withers\" entry \"annotateFields\"");
  }

  @Test
  void rejectsAnUnresolvedRef() {
    Path f = schema("x.Bad", "\"properties\": { \"a\": { \"$ref\": \"x.Ghost\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("x.Ghost");
  }

  @Test
  void aRefIntoTheJdkResolvesToAnExternalTypeNotASchema() {
    Path f =
        schema(
            "x.TimeRange",
            "\"properties\": { \"since\": { \"$ref\": \"java.time.Instant\" }, \"ids\": { \"type\":"
                + " \"array\", \"items\": { \"$ref\": \"java.util.UUID\" } } }");

    ModelType t = loader.load(List.of(f)).get(0);
    assertThat(t.getProperties().get(0).getType())
        .isEqualTo(PropType.ExternalType.of("java.time.Instant"));
    assertThat(t.getProperties().get(1).getType())
        .isEqualTo(PropType.ArrayType.of(PropType.ExternalType.of("java.util.UUID")));
  }

  @Test
  void rejectsAMissingSchemaVersion() {
    Path f =
        write(
            "Bad.json",
            "{ \"title\": \"Bad.Bad\", \"type\": \"object\", \"properties\": {}" + " }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("missing \"$modelmaker\"");
  }

  @Test
  void rejectsAnUnsupportedSchemaVersion() {
    Path f =
        write(
            "Bad.json",
            "{ \"$modelmaker\": \"v2.0\", \"title\": \"Bad.Bad\", \"type\": \"object\","
                + " \"properties\": {} }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unsupported \"$modelmaker\": \"v2.0\"");
  }

  @Test
  void rejectsAMissingTitle() {
    Path f =
        write(
            "Bad.json", "{ \"$modelmaker\": \"v1.0\", \"type\": \"object\", \"properties\": {} }");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("missing \"title\"");
  }

  @Test
  void rejectsANonFqcnTitle() {
    Path f =
        write(
            "Bad.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"Bad\", \"type\": \"object\", \"properties\":"
                + " {} }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("fully-qualified class name");
  }

  @Test
  void rejectsAFileNameThatDoesNotMatchTheTitle() {
    Path f =
        write(
            "wrong.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"com.example.Foo\", \"type\": \"object\","
                + " \"properties\": {} }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("must be named \"com.example.Foo.json\"");
  }

  @Test
  void rejectsANestedObjectTitleWithDots() {
    Path f =
        schema(
            "x.Bad",
            "\"properties\": { \"a\": { \"type\": \"object\", \"title\": \"a.b\", \"properties\": {"
                + " \"c\": { \"type\": \"string\" } } } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("must be a plain class name");
  }

  @Test
  void loadRejectsANullFileList() {
    assertThatNullPointerException().isThrownBy(() -> loader.load(null));
  }

  @Test
  void enumBecomesAStringWithAPatternAlternationNeverAJavaEnum() {
    Path f =
        schema(
            "x.Order",
            "\"properties\": { \"status\": { \"type\": \"string\", \"enum\": [\"NEW\", \"PAID\","
                + " \"SHIPPED\"] } }");
    ModelType m = loader.load(List.of(f)).get(0);
    Property status = prop(m, "status");

    assertThat(status.getType()).isEqualTo(PropType.ScalarType.STRING);
    assertThat(status.getConstraints().getPattern())
        .isEqualTo("^(\\QNEW\\E|\\QPAID\\E|\\QSHIPPED\\E)$");
    assertThat(status.getConstraints().getPatternMessage())
        .isEqualTo("must be one of NEW, PAID, SHIPPED");
  }

  @Test
  void anExplicitPatternOverridesEnum() {
    Path f =
        schema(
            "x.Order",
            "\"properties\": { \"status\": { \"type\": \"string\", \"enum\": [\"NEW\", \"PAID\"],"
                + " \"pattern\": \"^X\" } }");
    Constraints c = prop(loader.load(List.of(f)).get(0), "status").getConstraints();

    assertThat(c.getPattern()).isEqualTo("^X");
    assertThat(c.getPatternMessage()).isNull();
  }

  @Test
  void anEmptyEnumIsIgnored() {
    Path f =
        schema("x.Order", "\"properties\": { \"status\": { \"type\": \"string\", \"enum\": [] } }");
    Constraints c = prop(loader.load(List.of(f)).get(0), "status").getConstraints();

    assertThat(c.getPattern()).isNull();
    assertThat(c.getPatternMessage()).isNull();
  }

  @Test
  void integerDefaultsToIntFormatIntIsTheSameAsAbsentFormatLongIsDistinct() {
    Path f =
        schema(
            "x.N",
            """
            "properties": {
              "a": { "type": "integer" },
              "b": { "type": "integer", "format": "int" },
              "c": { "type": "integer", "format": "long" }
            }
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "a").getType()).isEqualTo(PropType.ScalarType.INTEGER);
    assertThat(prop(m, "b").getType()).isEqualTo(PropType.ScalarType.INTEGER);
    assertThat(prop(m, "c").getType()).isEqualTo(PropType.ScalarType.LONG);
  }

  @Test
  void rejectsAnUnsupportedIntegerFormat() {
    Path f =
        schema(
            "x.N", "\"properties\": { \"a\": { \"type\": \"integer\", \"format\": \"short\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unsupported format \"short\" for type \"integer\"");
  }

  @Test
  void numberDefaultsToDoubleFormatDoubleIsTheSameAsAbsentFormatFloatIsDistinct() {
    Path f =
        schema(
            "x.N",
            """
            "properties": {
              "a": { "type": "number" },
              "b": { "type": "number", "format": "double" },
              "c": { "type": "number", "format": "float" }
            }
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "a").getType()).isEqualTo(PropType.ScalarType.NUMBER);
    assertThat(prop(m, "b").getType()).isEqualTo(PropType.ScalarType.NUMBER);
    assertThat(prop(m, "c").getType()).isEqualTo(PropType.ScalarType.FLOAT);
  }

  @Test
  void rejectsAnUnsupportedNumberFormat() {
    Path f =
        schema("x.N", "\"properties\": { \"a\": { \"type\": \"number\", \"format\": \"half\" } }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unsupported format \"half\" for type \"number\"");
  }

  @Test
  void minMaxAndDecimalMinMaxApplyToTheWideFormatsToo() {
    Path f =
        schema(
            "x.N",
            """
            "properties": {
              "big":   { "type": "integer", "format": "long", "minimum": 0, "maximum": 10 },
              "small": { "type": "number", "format": "float", "minimum": 0, "maximum": 1 }
            }
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "big").getConstraints().getMinimum()).isEqualTo(0L);
    assertThat(prop(m, "big").getConstraints().getMaximum()).isEqualTo(10L);
    assertThat(prop(m, "small").getConstraints().getDecimalMinimum()).isEqualTo("0");
    assertThat(prop(m, "small").getConstraints().getDecimalMaximum()).isEqualTo("1");
  }
}
