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

import java.util.Collections;
import java.util.List;

/**
 * A common base for a {@link SnippetRenderer} implementations that need the standard {@code
 * (indent, properties, options, types)} shape.
 */
abstract sealed class AbstractSnippetRenderer implements SnippetRenderer
    permits BuilderRenderer,
        BuilderMutatorRenderer,
        ConstructorRenderer,
        FieldsRenderer,
        GettersRenderer,
        WithersRenderer {

  protected String indent = "";
  protected List<Property> properties = Collections.emptyList();
  protected ModelOptions options = ModelOptions.defaults();
  protected JavaTypes types = JavaTypes.defaults();

  /** Creates a new {@link AbstractSnippetRenderer}. */
  AbstractSnippetRenderer() {}

  /**
   * Fills {@link #indent}, {@link #properties}, {@link #options} and {@link #types} from {@code
   * type}, then delegates to {@link #initInternal} for any renderer-specific setup.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render.
   * @param options settings that decide boxed vs primitive, whether Jackson/validation annotations
   *     are emitted, and so on.
   * @return {@code this}, so the call can chain into {@link #render()}.
   */
  @Override
  public final SnippetRenderer init(String indent, ModelType type, ModelOptions options) {
    this.indent = indent;
    this.properties = List.copyOf(type.getProperties());
    this.options = options;
    this.types = new JavaTypes(type.getPackageName(), options);
    initInternal(indent, type, options);
    return this;
  }

  /**
   * Extension point for a subclass that also needs the type's own name or other setup beyond the
   * standard four fields. No-op by default.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render.
   * @param options settings passed through from {@link #init}.
   */
  protected void initInternal(String indent, ModelType type, ModelOptions options) {}
}
