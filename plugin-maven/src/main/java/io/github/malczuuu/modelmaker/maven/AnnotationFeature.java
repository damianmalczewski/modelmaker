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
 * A feature emitting annotations: on or off, plus where the annotations are placed. Defaults to off
 * and, once enabled, to the getters.
 */
public class AnnotationFeature extends ToggleFeature {

  /**
   * Creates a new {@link AnnotationFeature}, turned off and annotating the getters once enabled.
   */
  public AnnotationFeature() {}

  private boolean annotateFields = false;
  private boolean annotateGetters = true;

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
