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

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Emits a Kotlin extensions file for the Java models of a {@link ModelType} tree: a {@code
 * receiver.mutate { }} extension for every type (top-level and nested), taking a {@code Mutator.()
 * -> Unit} block and going through the Java {@code mutate()} member.
 *
 * <p>{@link #emit} is called once per schema. Each extension is rendered by {@link
 * MutateExtensionRenderer}; this class only lays out the file - {@code package}, imports and the
 * do-not-edit banner.
 */
public final class KotlinExtensionMaker implements ModelMaker {

  /** Creates a new {@link KotlinExtensionMaker}. */
  public KotlinExtensionMaker() {}

  /**
   * Renders one {@code mutate { }} extension per type in the tree (top-level and nested), laid out
   * with {@code package}, imports and the do-not-edit banner.
   *
   * @param type the schema-derived model to emit.
   * @return the generated file's full source text.
   */
  @Override
  public String emit(ModelType type) {
    Objects.requireNonNull(type, "type must not be null");

    Set<String> imports = new TreeSet<>();
    StringBuilder body = new StringBuilder();
    render(body, imports, type.getName(), type);

    StringBuilder out = new StringBuilder();
    out.append("package ").append(type.getPackageName()).append("\n\n");
    if (!body.isEmpty()) {
      if (!imports.isEmpty()) {
        for (String imp : imports) {
          out.append("import ").append(imp).append("\n");
        }
        out.append('\n');
      }
      out.append(Constants.GENERATED_FILE_NOTICE).append("\n\n");
    }
    out.append(body);
    return out.toString();
  }

  private void render(StringBuilder body, Set<String> imports, String path, ModelType type) {
    if (!type.getProperties().isEmpty()) {
      RenderResult extension = new MutateExtensionRenderer().init("", path).render();
      imports.addAll(extension.getImports());
      body.append(extension.getCode()).append('\n');
    }
    for (ModelType nested : type.getNested()) {
      render(body, imports, path + "." + nested.getName(), nested);
    }
  }
}
