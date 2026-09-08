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

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Maps a {@link Property}'s schema constraints to {@code jakarta.validation} annotations. Every
 * constraint annotation carries a hardcoded English {@code message = "..."}, so validation output
 * does not depend on the resource bundle / locale.
 */
final class ConstraintUtils {

  private static final String CONSTRAINTS = "jakarta.validation.constraints";

  private ConstraintUtils() {}

  /**
   * Maps one property's constraints to their {@code jakarta.validation} annotations.
   *
   * @param prop the property to map.
   * @return the annotations for {@code prop}, in a stable order.
   */
  static List<AnnotationSpec> constraintAnnotationsOf(Property prop) {
    List<AnnotationSpec> out = new ArrayList<>();
    Constraints c = prop.getConstraints();

    if (prop.nonNull()) {
      out.add(constraint("NotNull", "must not be null"));
    }
    if (prop.getType() instanceof PropType.RefType) {
      out.add(new AnnotationSpec("Valid", "jakarta.validation.Valid", List.of()));
    }
    AnnotationSpec items = sizeConstraint(c.getMinItems(), c.getMaxItems());
    if (items != null) {
      out.add(items);
    }
    appendValueConstraints(c, out);
    return out;
  }

  /**
   * Maps the constraints applied to each element of an {@code array} property to their {@code
   * jakarta.validation} annotations, for a container-element position (e.g. {@code List<@Size(...)
   * String>}). Never emits {@code @NotNull} or {@code @Valid}.
   *
   * @param elementConstraints the element constraints, or {@code null} when unset.
   * @return the element annotations, in a stable order; empty when {@code elementConstraints} is
   *     {@code null} or carries no facet.
   */
  static List<AnnotationSpec> elementConstraintAnnotationsOf(
      @Nullable Constraints elementConstraints) {
    List<AnnotationSpec> out = new ArrayList<>();
    if (elementConstraints != null) {
      appendValueConstraints(elementConstraints, out);
    }
    return out;
  }

  /**
   * Appends the value constraints shared by a property and a list element - {@code @Email},
   * {@code @Pattern}, {@code @Size} (from {@code minLength}/{@code maxLength}),
   * {@code @Min}/{@code @Max} and {@code @DecimalMin}/{@code @DecimalMax} - in a stable order.
   *
   * @param c the constraints to map.
   * @param out the list to append to.
   */
  private static void appendValueConstraints(Constraints c, List<AnnotationSpec> out) {
    if (c.isEmail()) {
      out.add(constraint("Email", "must be a well-formed email address"));
    }
    String pattern = c.getPattern();
    if (pattern != null) {
      String message = c.getPatternMessage();
      out.add(
          constraint(
              "Pattern",
              message != null ? message : "must match \"" + pattern + "\"",
              "regexp = \"" + escapeConstraintLiteral(pattern) + "\""));
    }
    AnnotationSpec length = sizeConstraint(c.getMinLength(), c.getMaxLength());
    if (length != null) {
      out.add(length);
    }
    Long minimum = c.getMinimum();
    if (minimum != null) {
      out.add(
          constraint("Min", "must be greater than or equal to " + minimum, "value = " + minimum));
    }
    Long maximum = c.getMaximum();
    if (maximum != null) {
      out.add(constraint("Max", "must be less than or equal to " + maximum, "value = " + maximum));
    }
    String decimalMinimum = c.getDecimalMinimum();
    if (decimalMinimum != null) {
      out.add(
          constraint(
              "DecimalMin",
              "must be greater than or equal to " + decimalMinimum,
              "value = \"" + decimalMinimum + "\""));
    }
    String decimalMaximum = c.getDecimalMaximum();
    if (decimalMaximum != null) {
      out.add(
          constraint(
              "DecimalMax",
              "must be less than or equal to " + decimalMaximum,
              "value = \"" + decimalMaximum + "\""));
    }
  }

  /**
   * Escapes a string for use inside a Java double-quoted literal (the escapes are the same in
   * Kotlin).
   *
   * @param s the raw string.
   * @return the escaped string.
   */
  static String escapeConstraintLiteral(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /**
   * A {@code jakarta.validation.constraints.<name>} annotation - the package is always the same, so
   * only the simple name is given. {@code args} come first, then the hardcoded {@code message}.
   *
   * @param name the annotation's simple name, e.g. {@code NotNull}.
   * @param message the hardcoded {@code message} value.
   * @param args pre-rendered annotation arguments, before {@code message}.
   * @return the built {@link AnnotationSpec}.
   */
  private static AnnotationSpec constraint(String name, String message, String... args) {
    List<String> all = new ArrayList<>(List.of(args));
    all.add("message = \"" + escapeConstraintLiteral(message) + "\"");
    return new AnnotationSpec(name, CONSTRAINTS + "." + name, List.copyOf(all));
  }

  private static @Nullable AnnotationSpec sizeConstraint(
      @Nullable Integer min, @Nullable Integer max) {
    if (min == null && max == null) {
      return null;
    }
    List<String> args = new ArrayList<>();
    if (min != null) {
      args.add("min = " + min);
    }
    if (max != null) {
      args.add("max = " + max);
    }
    String message;
    if (min != null && max != null) {
      message = "size must be between " + min + " and " + max;
    } else if (max != null) {
      message = "size must be at most " + max;
    } else {
      message = "size must be at least " + min;
    }
    return constraint("Size", message, args.toArray(new String[0]));
  }
}
