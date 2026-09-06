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

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultValueTest {

  @Nested
  class StrTest {

    @Test
    void equalWhenValueMatches() {
      assertThat(DefaultValue.Str.of("x"))
          .isEqualTo(DefaultValue.Str.of("x"))
          .hasSameHashCodeAs(DefaultValue.Str.of("x"));
    }

    @Test
    void notEqualWhenValueDiffers() {
      assertThat(DefaultValue.Str.of("x")).isNotEqualTo(DefaultValue.Str.of("y"));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(DefaultValue.Str.of("x")).isNotEqualTo(null).isNotEqualTo("x");
    }

    @Test
    void toStringReportsValue() {
      assertThat(DefaultValue.Str.of("x").toString()).isEqualTo("Str[value=x]");
    }
  }

  @Nested
  class NumTest {

    @Test
    void equalWhenLiteralMatches() {
      assertThat(DefaultValue.Num.of("1.0"))
          .isEqualTo(DefaultValue.Num.of("1.0"))
          .hasSameHashCodeAs(DefaultValue.Num.of("1.0"));
    }

    @Test
    void notEqualWhenLiteralDiffers() {
      assertThat(DefaultValue.Num.of("1.0")).isNotEqualTo(DefaultValue.Num.of("2.0"));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(DefaultValue.Num.of("1.0")).isNotEqualTo(null).isNotEqualTo("1.0");
    }

    @Test
    void toStringReportsLiteral() {
      assertThat(DefaultValue.Num.of("1.0").toString()).isEqualTo("Num[literal=1.0]");
    }
  }

  @Nested
  class BoolTest {

    @Test
    void equalWhenValueMatches() {
      assertThat(DefaultValue.Bool.of(true))
          .isEqualTo(DefaultValue.Bool.of(true))
          .hasSameHashCodeAs(DefaultValue.Bool.of(true));
    }

    @Test
    void notEqualWhenValueDiffers() {
      assertThat(DefaultValue.Bool.of(true)).isNotEqualTo(DefaultValue.Bool.of(false));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(DefaultValue.Bool.of(true)).isNotEqualTo(null).isNotEqualTo(true);
    }

    @Test
    void toStringReportsValue() {
      assertThat(DefaultValue.Bool.of(true).toString()).isEqualTo("Bool[value=true]");
    }
  }

  @Nested
  class ArrTest {

    @Test
    void equalWhenElementsMatch() {
      assertThat(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("x"))))
          .isEqualTo(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("x"))))
          .hasSameHashCodeAs(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("x"))));
    }

    @Test
    void notEqualWhenElementsDiffer() {
      assertThat(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("x"))))
          .isNotEqualTo(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("y"))));
    }

    @Test
    void notEqualToNullOrAnotherType() {
      assertThat(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("x"))))
          .isNotEqualTo(null)
          .isNotEqualTo(List.of(DefaultValue.Str.of("x")));
    }

    @Test
    void toStringReportsElements() {
      assertThat(DefaultValue.Arr.of(List.of(DefaultValue.Str.of("x"))).toString())
          .isEqualTo("Arr[elements=[Str[value=x]]]");
    }
  }
}
