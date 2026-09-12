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

class SchemaExceptionTest {

  @Test
  void carriesNoFileWhenNotAttributableToOne() {
    SchemaException exception = new SchemaException("something went wrong");

    assertThat(exception.getFile()).isNull();
    assertThat(exception).hasMessage("something went wrong").hasNoCause();
  }

  @Test
  void carriesTheFileAndTheCauseItWasBuiltWith() {
    Exception cause = new IllegalStateException("boom");
    SchemaException exception = new SchemaException("x.A.json", "x.A.json: broken", cause);

    assertThat(exception.getFile()).isEqualTo("x.A.json");
    assertThat(exception).hasMessage("x.A.json: broken").hasCause(cause);
  }

  @Test
  void reportsItselfAsTheOnlyErrorWhenNothingWasCollected() {
    SchemaException exception = new SchemaException("x.A.json", "x.A.json: broken", null);

    assertThat(exception.getErrors()).containsExactly(exception);
  }

  @Test
  void reportsTheCollectedErrorsWhenSeveralWereSuppressed() {
    SchemaException first = new SchemaException("x.A.json", "x.A.json: broken", null);
    SchemaException second = new SchemaException("x.B.json", "x.B.json: also broken", null);
    SchemaException aggregate = new SchemaException("2 schema errors");
    aggregate.addSuppressed(first);
    aggregate.addSuppressed(second);

    assertThat(aggregate.getFile()).isNull();
    assertThat(aggregate.getErrors()).isEqualTo(List.of(first, second));
  }

  @Test
  void ignoresSuppressedThrowablesThatAreNotSchemaErrors() {
    SchemaException exception = new SchemaException("x.A.json", "x.A.json: broken", null);
    exception.addSuppressed(new IllegalStateException("unrelated"));

    assertThat(exception.getErrors()).containsExactly(exception);
  }
}
