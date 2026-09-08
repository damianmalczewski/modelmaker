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

class ConstraintUtilsTest {

  private static List<String> render(Property prop) {
    return ConstraintUtils.constraintAnnotationsOf(prop).stream()
        .map(
            it ->
                it.getArgs().isEmpty()
                    ? it.getSimpleName()
                    : it.getSimpleName() + "(" + String.join(", ", it.getArgs()) + ")")
        .toList();
  }

  @Test
  void everyConstraintCarriesAHardcodedEnglishMessageValidDoesNot() {
    Property prop =
        new Property(
            "x",
            PropType.RefType.of("Address"),
            true,
            Constraints.builder().pattern("^a\"b\\c").minLength(2).decimalMinimum("0.5").build(),
            "x",
            null);

    List<String> rendered = render(prop);

    assertThat(rendered).contains("NotNull(message = \"must not be null\")");
    assertThat(rendered).contains("Valid");
    assertThat(rendered).contains("Size(min = 2, message = \"size must be at least 2\")");
    assertThat(rendered)
        .contains(
            "DecimalMin(value = \"0.5\", message = \"must be greater than or equal to 0.5\")");
    assertThat(rendered.stream().filter(it -> it.startsWith("Pattern")).findFirst().orElseThrow())
        .isEqualTo(
            "Pattern(regexp = \"^a\\\"b\\\\c\", message = \"must match \\\"^a\\\"b\\\\c\\\"\")");

    // @Valid alone, no message
    assertThat(
            ConstraintUtils.constraintAnnotationsOf(prop).stream()
                .filter(it -> it.getSimpleName().equals("Valid"))
                .findFirst()
                .orElseThrow()
                .getArgs())
        .isEmpty();
  }

  @Test
  void elementConstraintsMapToValueAnnotationsWithoutNotNullOrValid() {
    List<String> rendered =
        ConstraintUtils.elementConstraintAnnotationsOf(
                Constraints.builder().minLength(1).maxLength(255).pattern("^x").build())
            .stream()
            .map(
                it ->
                    it.getArgs().isEmpty()
                        ? it.getSimpleName()
                        : it.getSimpleName() + "(" + String.join(", ", it.getArgs()) + ")")
            .toList();

    assertThat(rendered)
        .containsExactly(
            "Pattern(regexp = \"^x\", message = \"must match \\\"^x\\\"\")",
            "Size(min = 1, max = 255, message = \"size must be between 1 and 255\")");
    assertThat(rendered).noneMatch(it -> it.startsWith("NotNull") || it.startsWith("Valid"));
  }

  @Test
  void elementConstraintsAreEmptyWhenNullOrBlank() {
    assertThat(ConstraintUtils.elementConstraintAnnotationsOf(null)).isEmpty();
    assertThat(ConstraintUtils.elementConstraintAnnotationsOf(Constraints.none())).isEmpty();
  }
}
