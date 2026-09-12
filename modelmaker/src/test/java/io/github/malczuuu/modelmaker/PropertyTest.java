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

import static io.github.malczuuu.modelmaker.PropertyFactory.property;
import static io.github.malczuuu.modelmaker.PropertyFactory.sensitiveProperty;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PropertyTest {

  private static Property full() {
    return property(
        "id",
        PropType.ScalarType.STRING,
        true,
        Constraints.builder().pattern("^X").build(),
        "id_json",
        DefaultValue.Str.of("x"));
  }

  @Test
  void equalWhenEveryFieldMatches() {
    assertThat(full()).isEqualTo(full()).hasSameHashCodeAs(full());
  }

  @Test
  void notEqualWhenNameDiffers() {
    Property other =
        property(
            "other",
            PropType.ScalarType.STRING,
            true,
            Constraints.builder().pattern("^X").build(),
            "id_json",
            DefaultValue.Str.of("x"));

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualWhenRequiredDiffers() {
    assertThat(property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null))
        .isNotEqualTo(
            property("id", PropType.ScalarType.STRING, false, Constraints.none(), "id", null));
  }

  @Test
  void notEqualWhenDefaultValueDiffers() {
    Property other =
        property(
            "id",
            PropType.ScalarType.STRING,
            true,
            Constraints.builder().pattern("^X").build(),
            "id_json",
            DefaultValue.Str.of("y"));

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(full()).isNotEqualTo(null).isNotEqualTo("id");
  }

  @Test
  void sixArgConstructorLeavesOpenApiMetadataUnset() {
    Property prop =
        property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null);

    assertThat(prop.getDescription()).isNull();
    assertThat(prop.getExample()).isNull();
  }

  @Test
  void carriesDescriptionAndExample() {
    Property prop =
        property(
            "id",
            PropType.ScalarType.STRING,
            true,
            Constraints.none(),
            "id",
            null,
            "the identifier",
            "P123");

    assertThat(prop.getDescription()).isEqualTo("the identifier");
    assertThat(prop.getExample()).isEqualTo("P123");
  }

  @Test
  void notEqualWhenDescriptionOrExampleDiffers() {
    Property base =
        property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null, "a", "x");

    assertThat(base)
        .isNotEqualTo(
            property(
                "id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null, "b", "x"))
        .isNotEqualTo(
            property(
                "id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null, "a", "y"));
  }

  @Test
  void toStringReportsEveryField() {
    assertThat(full().toString())
        .isEqualTo(
            "Property["
                + "name=id"
                + ", type=STRING"
                + ", required=true"
                + ", constraints="
                + Constraints.builder().pattern("^X").build()
                + ", jsonName=id_json"
                + ", defaultValue=Str[value=x]"
                + ", description=null"
                + ", example=null"
                + ", sensitive=false"
                + "]");
  }

  @Test
  void jsonNameDefaultsToNameWhenOmitted() {
    Property prop =
        property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null);

    assertThat(prop.getJsonName()).isEqualTo("id");
    assertThat(prop.getDefaultValue()).isNull();
  }

  @Test
  void defaultValueDefaultsToNullWhenOmittedButJsonNameGiven() {
    Property prop =
        property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id_json", null);

    assertThat(prop.getJsonName()).isEqualTo("id_json");
    assertThat(prop.getDefaultValue()).isNull();
  }

  @Test
  void jsonNameDefaultsToNameWhenOnlyDefaultValueGiven() {
    Property prop =
        property(
            "id",
            PropType.ScalarType.STRING,
            false,
            Constraints.none(),
            "id",
            DefaultValue.Str.of("x"));

    assertThat(prop.getJsonName()).isEqualTo("id");
    assertThat(prop.getDefaultValue()).isEqualTo(DefaultValue.Str.of("x"));
  }

  @Test
  void nonNullWhenRequired() {
    Property prop =
        property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null);

    assertThat(prop.nonNull()).isTrue();
  }

  @Test
  void nonNullWhenOptionalWithADefault() {
    Property prop =
        property(
            "id",
            PropType.ScalarType.STRING,
            false,
            Constraints.none(),
            "id",
            DefaultValue.Str.of("x"));

    assertThat(prop.nonNull()).isTrue();
  }

  @Test
  void notNonNullWhenOptionalWithNoDefault() {
    Property prop =
        property("id", PropType.ScalarType.STRING, false, Constraints.none(), "id", null);

    assertThat(prop.nonNull()).isFalse();
  }

  @Test
  void isNotSensitiveUnlessTheSchemaSaysSo() {
    assertThat(full().isSensitive()).isFalse();

    Property sensitive =
        sensitiveProperty("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null);

    assertThat(sensitive.isSensitive()).isTrue();
    assertThat(sensitive).isNotEqualTo(full());
  }
}
