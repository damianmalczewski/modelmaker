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

package io.github.malczuuu.modelmaker.maven.config;

/**
 * The {@code validation} feature: whether its {@code jakarta.validation} constraints are emitted,
 * and where they sit.
 *
 * <pre>
 * &lt;validation&gt;
 *   &lt;enabled&gt;true&lt;/enabled&gt;
 *   &lt;annotateFields&gt;false&lt;/annotateFields&gt;
 *   &lt;annotateGetters&gt;true&lt;/annotateGetters&gt;
 * &lt;/validation&gt;
 * </pre>
 *
 * <p>With both {@code annotateFields} and {@code annotateGetters} on, the annotations are emitted
 * on both.
 */
public class ValidationFeature {

  private boolean enabled = false;
  private boolean annotateFields = false;
  private boolean annotateGetters = true;

  /**
   * Creates a new {@link ValidationFeature}, turned off and annotating the getters once enabled.
   */
  public ValidationFeature() {}

  /**
   * Whether the feature emits any annotations.
   *
   * @return {@code true} when on.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Turns the feature on or off.
   *
   * @param enabled {@code true} to turn it on.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Whether the annotations are placed on the generated fields.
   *
   * @return {@code true} when they are.
   */
  public boolean isAnnotateFields() {
    return annotateFields;
  }

  /**
   * Places the annotations on the generated fields.
   *
   * @param annotateFields {@code true} to annotate the fields.
   */
  public void setAnnotateFields(boolean annotateFields) {
    this.annotateFields = annotateFields;
  }

  /**
   * Whether the annotations are placed on the generated getters.
   *
   * @return {@code true} when they are.
   */
  public boolean isAnnotateGetters() {
    return annotateGetters;
  }

  /**
   * Places the annotations on the generated getters.
   *
   * @param annotateGetters {@code true} to annotate the getters.
   */
  public void setAnnotateGetters(boolean annotateGetters) {
    this.annotateGetters = annotateGetters;
  }
}
