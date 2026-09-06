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

import org.junit.jupiter.api.Test;

class PropertyTest {

  private static Property full() {
    return new Property(
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
        new Property(
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
    assertThat(new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null))
        .isNotEqualTo(
            new Property("id", PropType.ScalarType.STRING, false, Constraints.none(), "id", null));
  }

  @Test
  void notEqualWhenDefaultValueDiffers() {
    Property other =
        new Property(
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
                + "]");
  }

  @Test
  void jsonNameDefaultsToNameWhenOmitted() {
    Property prop =
        new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null);

    assertThat(prop.getJsonName()).isEqualTo("id");
    assertThat(prop.getDefaultValue()).isNull();
  }

  @Test
  void defaultValueDefaultsToNullWhenOmittedButJsonNameGiven() {
    Property prop =
        new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id_json", null);

    assertThat(prop.getJsonName()).isEqualTo("id_json");
    assertThat(prop.getDefaultValue()).isNull();
  }

  @Test
  void jsonNameDefaultsToNameWhenOnlyDefaultValueGiven() {
    Property prop =
        new Property(
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
        new Property("id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null);

    assertThat(prop.nonNull()).isTrue();
  }

  @Test
  void nonNullWhenOptionalWithADefault() {
    Property prop =
        new Property(
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
        new Property("id", PropType.ScalarType.STRING, false, Constraints.none(), "id", null);

    assertThat(prop.nonNull()).isFalse();
  }
}
