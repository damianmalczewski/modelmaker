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

import java.util.Set;

/**
 * Renders the Kotlin {@code mutate { }} extension for one model type - a {@code receiver.mutate {
 * block }} function over the Java DTO's {@code BuilderMutator}, so {@code build()} stays out of the
 * block.
 *
 * <p>Renders just that one function (with its {@code @Generated} annotation); the {@code package}
 * declaration, the import list and the do-not-edit banner are the composing emitter's job.
 */
final class MutateExtensionRenderer implements SnippetRenderer {

  private static final String GENERATED_IMPORT = "javax.annotation.processing.Generated";

  private String indent = "";
  private String type = "";

  /** Creates a new {@link MutateExtensionRenderer}. */
  MutateExtensionRenderer() {}

  /**
   * Configures this renderer. Not an override of {@link SnippetRenderer#init(String, ModelType,
   * ModelOptions)} - this renders Kotlin, not a Java DTO, so there is no {@link ModelType} / {@link
   * ModelOptions} to take, and that method is left at its no-op default. This is the real
   * configuration method instead.
   *
   * @param indent leading whitespace prepended to every rendered line.
   * @param type the receiver type, dot-qualified for a nested type (e.g. {@code Order.Line}).
   * @return {@code this}, so the call can chain into {@link #render()}.
   */
  SnippetRenderer init(String indent, String type) {
    this.indent = indent;
    this.type = type;
    return this;
  }

  /**
   * Renders the {@code mutate { }} extension function.
   *
   * @return the rendered function, plus its imports.
   */
  @Override
  public RenderResult render() {
    String code =
        (indent + "@Generated(\"" + Constants.GENERATOR_NAME + "\")\n")
            + (indent
                + "public inline fun "
                + type
                + ".mutate(block: "
                + type
                + ".BuilderMutator.() -> Unit): "
                + type
                + " =\n")
            + (indent + "    this.mutate().also { it.block() }.build()\n");
    return new RenderResult(Set.of(GENERATED_IMPORT), code);
  }
}
