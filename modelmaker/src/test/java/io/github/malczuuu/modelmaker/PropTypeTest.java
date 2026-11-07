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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PropTypeTest {

  @Nested
  class ArrayTypeTest {

    @Test
    void equalWhenItemsMatch() {
      assertThat(PropType.ArrayType.of(PropType.ScalarType.STRING))
          .isEqualTo(PropType.ArrayType.of(PropType.ScalarType.STRING))
          .hasSameHashCodeAs(PropType.ArrayType.of(PropType.ScalarType.STRING));
    }

    @Test
    void notEqualWhenItemsDiffer() {
      assertThat(PropType.ArrayType.of(PropType.ScalarType.STRING))
          .isNotEqualTo(PropType.ArrayType.of(PropType.ScalarType.INTEGER));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(PropType.ArrayType.of(PropType.ScalarType.STRING))
          .isNotEqualTo(null)
          .isNotEqualTo(PropType.ScalarType.STRING);
    }

    @Test
    void toStringReportsItems() {
      assertThat(PropType.ArrayType.of(PropType.ScalarType.STRING).toString())
          .isEqualTo("ArrayType[items=STRING]");
    }
  }

  @Nested
  class RefTypeTest {

    @Test
    void equalWhenNameMatches() {
      assertThat(PropType.RefType.of("com.example.dto.Address"))
          .isEqualTo(PropType.RefType.of("com.example.dto.Address"))
          .hasSameHashCodeAs(PropType.RefType.of("com.example.dto.Address"));
    }

    @Test
    void notEqualWhenNameDiffers() {
      assertThat(PropType.RefType.of("com.example.dto.Address"))
          .isNotEqualTo(PropType.RefType.of("com.example.dto.Other"));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(PropType.RefType.of("com.example.dto.Address"))
          .isNotEqualTo(null)
          .isNotEqualTo("com.example.dto.Address");
    }

    @Test
    void toStringReportsName() {
      assertThat(PropType.RefType.of("com.example.dto.Address").toString())
          .isEqualTo("RefType[name=com.example.dto.Address]");
    }
  }

  @Nested
  class ExternalTypeTest {

    @Test
    void equalWhenQualifiedNameMatches() {
      assertThat(PropType.ExternalType.of("java.time.Instant"))
          .isEqualTo(PropType.ExternalType.of("java.time.Instant"))
          .hasSameHashCodeAs(PropType.ExternalType.of("java.time.Instant"));
    }

    @Test
    void notEqualWhenQualifiedNameDiffers() {
      assertThat(PropType.ExternalType.of("java.time.Instant"))
          .isNotEqualTo(PropType.ExternalType.of("java.util.UUID"));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(PropType.ExternalType.of("java.time.Instant"))
          .isNotEqualTo(null)
          .isNotEqualTo("java.time.Instant");
    }

    @Test
    void simpleNameReturnsTheLastSegment() {
      assertThat(PropType.ExternalType.of("java.time.Instant").simpleName()).isEqualTo("Instant");
    }

    @Test
    void toStringReportsQualifiedName() {
      assertThat(PropType.ExternalType.of("java.time.Instant").toString())
          .isEqualTo("ExternalType[qualifiedName=java.time.Instant]");
    }
  }
}
