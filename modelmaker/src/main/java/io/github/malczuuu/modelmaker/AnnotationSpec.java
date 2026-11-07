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

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One annotation to place on a generated field or getter. {@code args} are pre-rendered {@code
 * {name} = {value}} strings.
 */
final class AnnotationSpec {

  private final String simpleName;
  private final String importName;
  private final List<String> args;

  /**
   * Creates a new {@link AnnotationSpec}.
   *
   * @param simpleName the annotation's simple name, e.g. {@code NotNull}.
   * @param importName the annotation's fully-qualified name, for the import list.
   * @param args pre-rendered annotation arguments; empty for a marker annotation.
   */
  AnnotationSpec(String simpleName, String importName, List<String> args) {
    this.simpleName = simpleName;
    this.importName = importName;
    this.args = args;
  }

  /**
   * The annotation's simple name, e.g. {@code NotNull}.
   *
   * @return the simple name.
   */
  String getSimpleName() {
    return simpleName;
  }

  /**
   * The annotation's fully-qualified name, for the import list.
   *
   * @return the fully-qualified name.
   */
  String getImportName() {
    return importName;
  }

  /**
   * Pre-rendered annotation arguments; empty for a marker annotation.
   *
   * @return the arguments.
   */
  List<String> getArgs() {
    return args;
  }

  /**
   * The annotation in its {@code @Name} / {@code @Name(args)} source form.
   *
   * @return the rendered annotation.
   */
  String render() {
    return args.isEmpty()
        ? "@" + simpleName
        : "@" + simpleName + "(" + String.join(", ", args) + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AnnotationSpec other)) {
      return false;
    }
    return Objects.equals(simpleName, other.simpleName)
        && Objects.equals(importName, other.importName)
        && Objects.equals(args, other.args);
  }

  @Override
  public int hashCode() {
    return Objects.hash(simpleName, importName, args);
  }

  @Override
  public String toString() {
    return "AnnotationSpec["
        + ("simpleName=" + simpleName)
        + (", importName=" + importName)
        + (", args=" + args)
        + "]";
  }
}
