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

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when a schema cannot be loaded - malformed JSON, a missing or unsupported keyword, a
 * property name that does not normalize to a Java identifier, an unresolvable {@code $ref}, and so
 * on.
 *
 * <p>Instances come from the loaders: the constructors are package-private, as nothing outside this
 * package is in a position to report a schema problem.
 *
 * <p>{@link #getFile()} names the schema file the problem was found in, when it is attributable to
 * one. A load that turns up problems in several files throws one exception whose {@link
 * #getErrors()} lists them all, its {@link #getFile()} being {@code null}.
 */
public class SchemaException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final @Nullable String file;

  /**
   * Creates an exception not attributable to a single schema file.
   *
   * @param message the problem description.
   */
  SchemaException(String message) {
    this(null, message, null);
  }

  /**
   * Creates an exception for a problem found in one schema file.
   *
   * @param file the schema file's name, or {@code null} when not attributable to one.
   * @param message the problem description, expected to already name {@code file}.
   * @param cause the underlying failure, if any.
   */
  SchemaException(@Nullable String file, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.file = file;
  }

  /**
   * The schema file this problem was found in.
   *
   * @return the file name, or {@code null} when the problem spans files (see {@link #getErrors()})
   *     or is not attributable to one.
   */
  public @Nullable String getFile() {
    return file;
  }

  /**
   * Bundles several problems into one exception: the first when there is only one, otherwise a new
   * exception listing every message, with each problem attached as a suppressed exception.
   *
   * @param errors the collected problems, at least one.
   * @return the exception to throw.
   */
  static SchemaException of(List<SchemaException> errors) {
    if (errors.size() == 1) {
      return errors.get(0);
    }
    StringBuilder message = new StringBuilder(errors.size() + " schema errors:");
    for (SchemaException error : errors) {
      message.append("\n  - ").append(error.getMessage());
    }
    SchemaException aggregate = new SchemaException(message.toString());
    errors.forEach(aggregate::addSuppressed);
    return aggregate;
  }

  /**
   * The individual problems this exception reports.
   *
   * @return the collected problems for an exception aggregating several, otherwise a single-element
   *     list holding this exception.
   */
  public List<SchemaException> getErrors() {
    List<SchemaException> errors = new ArrayList<>();
    for (Throwable suppressed : getSuppressed()) {
      if (suppressed instanceof SchemaException error) {
        errors.add(error);
      }
    }
    return errors.isEmpty() ? List.of(this) : List.copyOf(errors);
  }
}
