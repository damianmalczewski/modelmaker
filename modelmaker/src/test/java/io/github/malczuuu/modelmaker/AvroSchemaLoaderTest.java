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
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AvroSchemaLoaderTest {

  private final SchemaLoader loader = new AvroSchemaLoader();

  @TempDir Path dir;

  /** Writes {@code <namespace>.<name>.avsc} so the file name matches, as the loader requires. */
  private Path schema(String namespace, String name, String body) {
    return write(
        namespace + "." + name + ".avsc",
        "{ \"type\": \"record\", \"namespace\": \""
            + namespace
            + "\", \"name\": \""
            + name
            + "\", "
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

  private static Property prop(ModelType type, String name) {
    return type.getProperties().stream()
        .filter(p -> p.getName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void parsesAFlatRecordWithRequiredAndNullableFields() {
    Path f =
        schema(
            "com.example.dto",
            "Person",
            """
            "fields": [
              { "name": "id", "type": "string" },
              { "name": "nickname", "type": ["null", "string"], "default": null }
            ]
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(m.getName()).isEqualTo("Person");
    assertThat(m.getPackageName()).isEqualTo("com.example.dto");
    assertThat(prop(m, "id").isRequired()).isTrue();
    assertThat(prop(m, "id").getType()).isEqualTo(PropType.ScalarType.STRING);
    assertThat(prop(m, "nickname").isRequired()).isFalse();
    assertThat(prop(m, "nickname").getDefaultValue()).isNull();
  }

  @Test
  void intAndLongAreDistinctTypesDefaultIsInt() {
    Path f =
        schema(
            "x",
            "N",
            """
            "fields": [
              { "name": "a", "type": "int" },
              { "name": "b", "type": "long" }
            ]
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "a").getType()).isEqualTo(PropType.ScalarType.INTEGER);
    assertThat(prop(m, "b").getType()).isEqualTo(PropType.ScalarType.LONG);
  }

  @Test
  void floatAndDoubleAreDistinctTypes() {
    Path f =
        schema(
            "x",
            "N",
            """
            "fields": [
              { "name": "a", "type": "float" },
              { "name": "b", "type": "double" }
            ]
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "a").getType()).isEqualTo(PropType.ScalarType.FLOAT);
    assertThat(prop(m, "b").getType()).isEqualTo(PropType.ScalarType.NUMBER);
  }

  @Test
  void bytesMapsToBytesAndBooleanMapsToBoolean() {
    Path f =
        schema(
            "x",
            "N",
            """
            "fields": [
              { "name": "a", "type": "bytes" },
              { "name": "b", "type": "boolean" }
            ]
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "a").getType()).isEqualTo(PropType.ScalarType.BYTES);
    assertThat(prop(m, "b").getType()).isEqualTo(PropType.ScalarType.BOOLEAN);
  }

  @Test
  void rejectsARecordThatDeclaresTheSameFieldTwice() {
    Path f =
        schema(
            "x",
            "N",
            """
            "fields": [
              { "name": "a", "type": "string" },
              { "name": "a", "type": "int" }
            ]
            """);

    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("field \"a\" is declared twice");
  }

  @Test
  void aNullableBytesUnionParsesToAnOptionalBytesField() {
    Path f =
        schema(
            "x",
            "N",
            """
            "fields": [
              { "name": "a", "type": ["null", "bytes"], "default": null }
            ]
            """);

    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(prop(m, "a").getType()).isEqualTo(PropType.ScalarType.BYTES);
    assertThat(prop(m, "a").isRequired()).isFalse();
  }

  @Test
  void enumBecomesAStringWithAPatternAlternationNeverAJavaEnum() {
    Path f =
        schema(
            "x",
            "Order",
            """
            "fields": [
              { "name": "status", "type": { "type": "enum", "name": "Status",
                "symbols": ["NEW", "PAID", "SHIPPED"] } }
            ]
            """);
    ModelType m = loader.load(List.of(f)).get(0);
    Property status = prop(m, "status");

    assertThat(status.getType()).isEqualTo(PropType.ScalarType.STRING);
    assertThat(status.getConstraints().getPattern())
        .isEqualTo("^(\\QNEW\\E|\\QPAID\\E|\\QSHIPPED\\E)$");
    assertThat(status.getConstraints().getPatternMessage())
        .isEqualTo("must be one of NEW, PAID, SHIPPED");
  }

  @Test
  void aNestedRecordBecomesANestedModelType() {
    Path f =
        schema(
            "x",
            "Order",
            """
            "fields": [
              { "name": "shipTo", "type": { "type": "record", "name": "Address",
                "fields": [ { "name": "city", "type": "string" } ] } }
            ]
            """);
    ModelType order = loader.load(List.of(f)).get(0);

    assertThat(order.getNested().stream().map(ModelType::getName)).containsExactly("Address");
    assertThat(prop(order, "shipTo").getType()).isEqualTo(PropType.RefType.of("Address"));
    assertThat(order.getNested().get(0).getPackageName()).isEqualTo("x");
  }

  @Test
  void parsesATopLevelFeaturesOverride() {
    Path f =
        write(
            "x.All.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"All\", \"features\": {"
                + " \"jackson\": { \"enabled\": true, \"annotateFields\": true },"
                + " \"validation\": { \"enabled\": false }, \"withers\": { \"enabled\": true },"
                + " \"preferPrimitives\": { \"enabled\": false },"
                + " \"openApi\": { \"enabled\": true } }, \"fields\": [] }");
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(m.getFeatureOverrides())
        .isEqualTo(
            FeatureOverrides.builder()
                .jackson(new JacksonOverride(true, true, null))
                .validation(ValidationOverride.enabled(false))
                .withers(true)
                .preferPrimitives(false)
                .openApi(OpenApiOverride.enabled(true))
                .build());
  }

  @Test
  void mapsFieldDocToThePropertyDescription() {
    Path f =
        write(
            "x.Account.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Account\", \"fields\": ["
                + " { \"name\": \"id\", \"type\": \"string\", \"doc\": \"the account id\" },"
                + " { \"name\": \"plain\", \"type\": \"string\" } ] }");
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(
            m.getProperties().stream()
                .filter(p -> p.getName().equals("id"))
                .findFirst()
                .orElseThrow()
                .getDescription())
        .isEqualTo("the account id");
    assertThat(
            m.getProperties().stream()
                .filter(p -> p.getName().equals("plain"))
                .findFirst()
                .orElseThrow()
                .getDescription())
        .isNull();
  }

  @Test
  void aSchemaWithNoFeaturesNodeHasNoOverrides() {
    Path f = schema("x", "None", "\"fields\": []");
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(m.getFeatureOverrides()).isEqualTo(FeatureOverrides.none());
  }

  @Test
  void aNestedRecordNeverInheritsTheTopLevelFeaturesOverride() {
    Path f =
        write(
            "x.Order.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Order\", \"features\": {"
                + " \"jackson\": { \"enabled\": true } }, \"fields\": [ { \"name\": \"shipTo\","
                + " \"type\": {"
                + " \"type\": \"record\", \"name\": \"Address\", \"fields\": [ { \"name\":"
                + " \"city\", \"type\": \"string\" } ] } } ] }");
    List<ModelType> types = loader.load(List.of(f));
    ModelType order = types.get(0);

    assertThat(order.getFeatureOverrides())
        .isEqualTo(FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build());
    assertThat(order.getNested().get(0).getFeatureOverrides()).isEqualTo(FeatureOverrides.none());
  }

  @Test
  void rejectsANonBooleanFeaturesOverride() {
    Path f =
        write(
            "x.Bad.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Bad\", \"features\": {"
                + " \"jackson\": { \"enabled\": \"yes\" } }, \"fields\": [] }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"features.jackson.enabled\" must be a boolean");
  }

  @Test
  void rejectsABooleanShorthandFeaturesEntry() {
    Path f =
        write(
            "x.Bad.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Bad\", \"features\": {"
                + " \"jackson\": true }, \"fields\": [] }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"features.jackson\" must be an object");
  }

  @Test
  void anArrayFieldBecomesAnArrayType() {
    Path f =
        schema(
            "x",
            "Order",
            "\"fields\": [ { \"name\": \"tags\", \"type\": { \"type\": \"array\", \"items\":"
                + " \"string\" } } ]");
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "tags").getType())
        .isEqualTo(PropType.ArrayType.of(PropType.ScalarType.STRING));
  }

  @Test
  void aNamedTypeReferenceResolvesAcrossFiles() {
    schema("x", "Address", "\"fields\": [ { \"name\": \"city\", \"type\": \"string\" } ]");
    Path f =
        schema("x", "Order", "\"fields\": [ { \"name\": \"shipTo\", \"type\": \"x.Address\" } ]");

    List<ModelType> types = loader.load(List.of(f, dir.resolve("x.Address.avsc")));
    ModelType order =
        types.stream().filter(t -> t.getName().equals("Order")).findFirst().orElseThrow();

    assertThat(prop(order, "shipTo").getType()).isEqualTo(PropType.RefType.of("x.Address"));
  }

  @Test
  void rejectsAnUnresolvedNamedTypeReference() {
    Path f = schema("x", "Order", "\"fields\": [ { \"name\": \"a\", \"type\": \"x.Ghost\" } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("x.Ghost");
  }

  @Test
  void parsesDefaultValuesForNullableFields() {
    Path f =
        schema(
            "x",
            "D",
            """
            "fields": [
              { "name": "active", "type": ["null", "boolean"], "default": true },
              { "name": "currency", "type": ["null", "string"], "default": "USD" },
              { "name": "roles", "type": ["null", { "type": "array", "items": "string" }],
                "default": ["user"] }
            ]
            """);
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "active").getDefaultValue()).isEqualTo(DefaultValue.Bool.of(true));
    assertThat(prop(m, "currency").getDefaultValue()).isEqualTo(DefaultValue.Str.of("USD"));
    assertThat(prop(m, "roles").getDefaultValue())
        .isEqualTo(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("user"))));
    assertThat(prop(m, "active").nonNull()).isTrue();
  }

  @Test
  void rejectsMap() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": { \"type\": \"map\", \"values\":"
                + " \"string\" } } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"map\" is not supported");
  }

  @Test
  void rejectsFixed() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": { \"type\": \"fixed\", \"name\": \"Md5\","
                + " \"size\": 16 } } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("\"fixed\" is not supported");
  }

  @Test
  void rejectsAMultiBranchUnion() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"string\", \"int\"] } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("unsupported union");
  }

  @Test
  void rejectsANonNullableUnion() {
    Path f =
        schema("x", "D", "\"fields\": [ { \"name\": \"a\", \"type\": [\"string\", \"int\"] } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("unsupported union");
  }

  @Test
  void ignoresLogicalTypeAndUsesTheBaseAvroType() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"at\", \"type\": { \"type\": \"long\", \"logicalType\":"
                + " \"timestamp-millis\" } } ]");
    ModelType m = loader.load(List.of(f)).get(0);

    assertThat(prop(m, "at").getType()).isEqualTo(PropType.ScalarType.LONG);
  }

  @Test
  void rejectsInvalidJson() {
    Path f = write("x.Bad.avsc", "{ not json");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("not valid JSON");
  }

  @Test
  void rejectsARootThatIsNotAJsonObject() {
    Path f = write("x.Bad.avsc", "[]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("root must be a JSON object");
  }

  @Test
  void rejectsARecordMissingFieldsArray() {
    Path f =
        write("x.Bad.avsc", "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Bad\" }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("missing \"fields\" array");
  }

  @Test
  void rejectsAFieldThatIsNotAnObject() {
    Path f =
        write(
            "x.Bad.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Bad\", \"fields\": ["
                + " \"nope\" ] }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("each field must be an object");
  }

  @Test
  void rejectsAFieldMissingType() {
    Path f = schema("x", "D", "\"fields\": [ { \"name\": \"a\" } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("missing \"type\"");
  }

  @Test
  void rejectsATypeNodeThatIsNeitherStringNorObject() {
    Path f = schema("x", "D", "\"fields\": [ { \"name\": \"a\", \"type\": 5 } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("unsupported type");
  }

  @Test
  void rejectsATypeObjectMissingItsOwnType() {
    Path f = schema("x", "D", "\"fields\": [ { \"name\": \"a\", \"type\": {} } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("needs \"type\"");
  }

  @Test
  void rejectsAnArrayMissingItems() {
    Path f =
        schema("x", "D", "\"fields\": [ { \"name\": \"a\", \"type\": { \"type\": \"array\" } } ]");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("array needs \"items\"");
  }

  @Test
  void rejectsNestedArrays() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": { \"type\": \"array\", \"items\": {"
                + " \"type\": \"array\", \"items\": \"string\" } } } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("nested arrays are not supported");
  }

  @Test
  void rejectsAnEnumMissingSymbols() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": { \"type\": \"enum\", \"name\": \"E\" } }"
                + " ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("enum needs \"symbols\"");
  }

  @Test
  void rejectsAStringDefaultOfTheWrongType() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"string\"],"
                + " \"default\": 5 } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be a string");
  }

  @Test
  void parsesAValidIntegerOrLongDefault() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"int\"], \"default\": 42 } ]");
    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(prop(m, "a").getDefaultValue()).isEqualTo(DefaultValue.Num.of("42"));
  }

  @Test
  void rejectsAnIntegerDefaultOfTheWrongType() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"int\"], \"default\": \"x\" }"
                + " ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be an integer");
  }

  @Test
  void parsesAValidFloatOrDoubleDefault() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"double\"], \"default\": 1.5 }"
                + " ]");
    ModelType m = loader.load(List.of(f)).get(0);
    assertThat(prop(m, "a").getDefaultValue()).isEqualTo(DefaultValue.Num.of("1.5"));
  }

  @Test
  void rejectsANumberDefaultOfTheWrongType() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"double\"], \"default\": \"x\""
                + " } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be a number");
  }

  @Test
  void rejectsABooleanDefaultOfTheWrongType() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", \"boolean\"], \"default\":"
                + " \"x\" } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be a boolean");
  }

  @Test
  void rejectsAnArrayDefaultThatIsNotAnArray() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", { \"type\": \"array\","
                + " \"items\": \"string\" }], \"default\": \"x\" } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default must be an array");
  }

  @Test
  void rejectsAnArrayDefaultWithNonScalarItems() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", { \"type\": \"array\","
                + " \"items\": { \"type\": \"record\", \"name\": \"E\", \"fields\": [] } }],"
                + " \"default\": [] } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("array default is only supported for scalar items");
  }

  @Test
  void rejectsAnArrayDefaultElementThatIsNull() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", { \"type\": \"array\","
                + " \"items\": \"string\" }], \"default\": [\"x\", null] } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("array default elements must not be null");
  }

  @Test
  void rejectsAnUnsupportedDefaultType() {
    Path f =
        schema(
            "x",
            "D",
            "\"fields\": [ { \"name\": \"a\", \"type\": [\"null\", { \"type\": \"record\","
                + " \"name\": \"E\", \"fields\": [] }], \"default\": {} } ]");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("default is not supported for this type");
  }

  @Test
  void rejectsATopLevelTypeThatIsNotARecord() {
    Path f = write("x.Bad.avsc", "{ \"type\": \"enum\", \"name\": \"Bad\", \"symbols\": [\"A\"] }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("top-level \"type\" must be \"record\"");
  }

  @Test
  void rejectsAMissingNamespace() {
    Path f = write("Bad.avsc", "{ \"type\": \"record\", \"name\": \"Bad\", \"fields\": [] }");
    assertThatThrownBy(() -> loader.load(List.of(f))).hasMessageContaining("missing \"namespace\"");
  }

  @Test
  void rejectsAFileNameThatDoesNotMatchNamespaceAndName() {
    Path f =
        write(
            "wrong.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Bad\", \"fields\": [] }");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("must be named \"x.Bad.avsc\"");
  }

  @Test
  void loadRejectsANullFileList() {
    assertThatNullPointerException().isThrownBy(() -> loader.load(null));
  }

  @Test
  void parsesTheSensitiveFieldAttribute() {
    Path f =
        write(
            "x.Token.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Token\", \"fields\": ["
                + " { \"name\": \"secret\", \"type\": \"string\", \"sensitive\": true },"
                + " { \"name\": \"name\", \"type\": \"string\" } ] }");

    List<ModelType> types = loader.load(List.of(f));

    assertThat(types.get(0).getProperties())
        .extracting(Property::getName, Property::isSensitive)
        .containsExactly(tuple("secret", true), tuple("name", false));
  }
}
