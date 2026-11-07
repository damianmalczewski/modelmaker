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
import org.junit.jupiter.api.Test;

class AnnotationSpecTest {

  private static AnnotationSpec full() {
    return new AnnotationSpec(
        "NotNull",
        "jakarta.validation.constraints.NotNull",
        List.of("message = \"must not be null\""));
  }

  @Test
  void equalWhenEveryFieldMatches() {
    assertThat(full()).isEqualTo(full()).hasSameHashCodeAs(full());
  }

  @Test
  void notEqualWhenSimpleNameDiffers() {
    AnnotationSpec other =
        new AnnotationSpec(
            "Email",
            "jakarta.validation.constraints.NotNull",
            List.of("message = \"must not be null\""));

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualWhenArgsDiffer() {
    AnnotationSpec other =
        new AnnotationSpec("NotNull", "jakarta.validation.constraints.NotNull", List.of());

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(full()).isNotEqualTo(null).isNotEqualTo("NotNull");
  }

  @Test
  void toStringReportsEveryField() {
    assertThat(full().toString())
        .isEqualTo(
            "AnnotationSpec["
                + "simpleName=NotNull"
                + ", importName=jakarta.validation.constraints.NotNull"
                + ", args=[message = \"must not be null\"]"
                + "]");
  }
}
