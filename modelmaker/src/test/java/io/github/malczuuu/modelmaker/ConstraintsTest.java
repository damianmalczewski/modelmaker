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

class ConstraintsTest {

  private static Constraints full() {
    return Constraints.builder()
        .pattern("^X")
        .patternMessage("must match")
        .minLength(1)
        .maxLength(10)
        .email(true)
        .minimum(0L)
        .maximum(100L)
        .decimalMinimum("0")
        .decimalMaximum("99.9")
        .minItems(1)
        .maxItems(5)
        .elementConstraints(Constraints.builder().minLength(1).maxLength(255).build())
        .build();
  }

  @Test
  void noneEqualsAnEmptyBuiltInstance() {
    assertThat(Constraints.none())
        .isEqualTo(Constraints.builder().build())
        .hasSameHashCodeAs(Constraints.builder().build());
  }

  @Test
  void equalWhenEveryFieldMatches() {
    assertThat(full()).isEqualTo(full()).hasSameHashCodeAs(full());
  }

  @Test
  void notEqualWhenPatternDiffers() {
    Constraints other = Constraints.builder().pattern("^Y").build();

    assertThat(Constraints.builder().pattern("^X").build()).isNotEqualTo(other);
  }

  @Test
  void notEqualWhenEmailDiffers() {
    assertThat(Constraints.builder().email(true).build())
        .isNotEqualTo(Constraints.builder().email(false).build());
  }

  @Test
  void notEqualWhenMinItemsDiffers() {
    assertThat(Constraints.builder().minItems(1).build())
        .isNotEqualTo(Constraints.builder().minItems(2).build());
  }

  @Test
  void notEqualWhenElementConstraintsDiffer() {
    assertThat(
            Constraints.builder()
                .elementConstraints(Constraints.builder().minLength(1).build())
                .build())
        .isNotEqualTo(
            Constraints.builder()
                .elementConstraints(Constraints.builder().minLength(2).build())
                .build());
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(full()).isNotEqualTo(null).isNotEqualTo("Constraints");
  }

  @Test
  void toStringReportsEveryField() {
    assertThat(full().toString())
        .isEqualTo(
            "Constraints["
                + "pattern=^X"
                + ", patternMessage=must match"
                + ", minLength=1"
                + ", maxLength=10"
                + ", email=true"
                + ", minimum=0"
                + ", maximum=100"
                + ", decimalMinimum=0"
                + ", decimalMaximum=99.9"
                + ", minItems=1"
                + ", maxItems=5"
                + ", elementConstraints=Constraints[pattern=null, patternMessage=null, minLength=1,"
                + " maxLength=255, email=false, minimum=null, maximum=null, decimalMinimum=null,"
                + " decimalMaximum=null, minItems=null, maxItems=null, elementConstraints=null]"
                + "]");
  }
}
