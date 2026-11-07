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

/**
 * Common contract for every {@code *SnippetRenderer}: default-constructed, then configured via
 * {@link #init(String, ModelType, ModelOptions)} (which returns {@code this}, so construction and
 * configuration chain in one expression), then rendered via {@link #render()}. Produces one chunk
 * of generated source, plus the imports it needs, as a {@link RenderResult}. Each implementation
 * renders one focused piece (a field list, a constructor, a whole nested class, ...); composing
 * them into a full file is the caller's job.
 *
 * <p>{@link #init} takes the shape most renderers need (an indent, the {@link ModelType}, the
 * {@link ModelOptions}); a renderer whose configuration doesn't fit that shape - {@link
 * MutateExtensionRenderer}'s Kotlin-side {@code (indent, dottedTypeName)}, say - is under no
 * obligation to override it: the default here is a no-op, and that renderer defines its own
 * differently-shaped {@code init} instead.
 */
sealed interface SnippetRenderer
    permits AbstractSnippetRenderer,
        BuilderFactoryRenderer,
        MutateExtensionRenderer,
        MutateMethodRenderer,
        SupportMethodsRenderer {

  /**
   * Configures this renderer, overwriting any earlier configuration. The default implementation is
   * a no-op, for a renderer whose real configuration doesn't fit this shape and so is done through
   * a method of its own instead.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the model type to render.
   * @param options settings that decide boxed vs primitive, whether Jackson/validation annotations
   *     are emitted, and so on. Ignored by a renderer that has no use for it.
   * @return {@code this}, so the call can chain into {@link #render()}.
   */
  default SnippetRenderer init(String indent, ModelType type, ModelOptions options) {
    return this;
  }

  /**
   * Renders this snippet, using whatever configuration {@link #init} captured.
   *
   * @return the rendered source chunk, plus the imports it needs.
   */
  RenderResult render();
}
