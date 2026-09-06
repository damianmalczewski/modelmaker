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

import java.util.Set;
import org.junit.jupiter.api.Test;

class RenderResultTest {

  private static RenderResult full() {
    return new RenderResult(Set.of("java.util.List"), "private final List<String> tags;\n");
  }

  @Test
  void equalWhenEveryFieldMatches() {
    assertThat(full()).isEqualTo(full()).hasSameHashCodeAs(full());
  }

  @Test
  void notEqualWhenCodeDiffers() {
    RenderResult other = new RenderResult(Set.of("java.util.List"), "different code");

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualWhenImportsDiffer() {
    RenderResult other =
        new RenderResult(Set.of("java.util.Set"), "private final List<String> tags;\n");

    assertThat(full()).isNotEqualTo(other);
  }

  @Test
  void notEqualToNullOrAnotherType() {
    assertThat(full()).isNotEqualTo(null).isNotEqualTo("code");
  }

  @Test
  void toStringReportsEveryField() {
    assertThat(full().toString())
        .isEqualTo(
            "RenderResult["
                + "imports=[java.util.List]"
                + ", code=private final List<String> tags;\n"
                + "]");
  }
}
