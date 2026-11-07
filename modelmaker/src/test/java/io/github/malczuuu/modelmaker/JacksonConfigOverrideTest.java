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

class JacksonOverrideTest {

  @Test
  void noneIsEmpty() {
    JacksonOverride none = JacksonOverride.none();

    assertThat(none.isEmpty()).isTrue();
    assertThat(none.getEnabled()).isEmpty();
    assertThat(none.getAnnotateFields()).isEmpty();
    assertThat(none.getAnnotateGetters()).isEmpty();
  }

  @Test
  void enabledFactorySetsOnlyEnabled() {
    JacksonOverride o = JacksonOverride.enabled(false);

    assertThat(o.getEnabled()).contains(false);
    assertThat(o.getAnnotateFields()).isEmpty();
    assertThat(o.getAnnotateGetters()).isEmpty();
    assertThat(o.isEmpty()).isFalse();
  }

  @Test
  void equalityAndToString() {
    assertThat(JacksonOverride.enabled(true))
        .isEqualTo(new JacksonOverride(true, null, null, null))
        .hasSameHashCodeAs(new JacksonOverride(true, null, null, null))
        .isNotEqualTo(JacksonOverride.none())
        .isNotEqualTo(null);

    assertThat(JacksonOverride.none().toString())
        .isEqualTo(
            "JacksonOverride[enabled=null, annotateFields=null, annotateGetters=null, includeNonNull=null]");
  }
}
