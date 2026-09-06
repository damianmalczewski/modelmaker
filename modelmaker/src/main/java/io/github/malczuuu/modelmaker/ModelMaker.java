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
 * Renders a {@link ModelType} tree, parsed from one schema file, to a generated source file's text.
 * Implemented per target language - see {@link JavaModelMaker} and {@link KotlinExtensionMaker}.
 * Not sealed - implement it for a target language of your own.
 */
public sealed interface ModelMaker permits JavaModelMaker, KotlinExtensionMaker {

  /**
   * Renders {@code type} (and, recursively, its {@link ModelType#getNested()} types) to source
   * code.
   *
   * @param type the schema-derived model to emit.
   * @return the generated file's full source text, including its {@code package} declaration.
   */
  String emit(ModelType type);
}
