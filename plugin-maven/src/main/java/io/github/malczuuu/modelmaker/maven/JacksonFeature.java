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

package io.github.malczuuu.modelmaker.maven;

/**
 * The {@code jackson} feature, which carries one option more than the other annotation features.
 */
public class JacksonFeature extends AnnotationFeature {

  /** Creates a new {@link JacksonFeature}, turned off. */
  public JacksonFeature() {}

  private boolean includeNonNull = false;

  /**
   * Whether {@code @JsonInclude(JsonInclude.Include.NON_NULL)} is emitted on the generated class.
   *
   * @return {@code true} when it is.
   */
  public boolean isIncludeNonNull() {
    return includeNonNull;
  }

  /**
   * Emits {@code @JsonInclude(JsonInclude.Include.NON_NULL)} on the generated class.
   *
   * @param includeNonNull {@code true} to emit it.
   */
  public void setIncludeNonNull(boolean includeNonNull) {
    this.includeNonNull = includeNonNull;
  }
}
