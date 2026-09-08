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
import org.jspecify.annotations.Nullable;

/**
 * Validation keywords carried by a property, all optional. Built through {@link #builder()} or
 * {@link #none()}.
 */
public final class Constraints {

  private static final Constraints NONE =
      new Constraints(null, null, null, null, false, null, null, null, null, null, null, null);

  private final @Nullable String pattern;
  private final @Nullable String patternMessage;
  private final @Nullable Integer minLength;
  private final @Nullable Integer maxLength;
  private final boolean email;
  private final @Nullable Long minimum;
  private final @Nullable Long maximum;
  private final @Nullable String decimalMinimum;
  private final @Nullable String decimalMaximum;
  private final @Nullable Integer minItems;
  private final @Nullable Integer maxItems;
  private final @Nullable Constraints elementConstraints;

  private Constraints(
      @Nullable String pattern,
      @Nullable String patternMessage,
      @Nullable Integer minLength,
      @Nullable Integer maxLength,
      boolean email,
      @Nullable Long minimum,
      @Nullable Long maximum,
      @Nullable String decimalMinimum,
      @Nullable String decimalMaximum,
      @Nullable Integer minItems,
      @Nullable Integer maxItems,
      @Nullable Constraints elementConstraints) {
    this.pattern = pattern;
    this.patternMessage = patternMessage;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.email = email;
    this.minimum = minimum;
    this.maximum = maximum;
    this.decimalMinimum = decimalMinimum;
    this.decimalMaximum = decimalMaximum;
    this.minItems = minItems;
    this.maxItems = maxItems;
    this.elementConstraints = elementConstraints;
  }

  /**
   * An empty constraint set, every flag unset.
   *
   * @return the shared empty instance.
   */
  public static Constraints none() {
    return NONE;
  }

  /**
   * Starts accumulating a new constraint set.
   *
   * @return a new {@link Builder}, every flag starting unset.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Regex for {@code @Pattern}; from an explicit {@code pattern} or a {@code string} format.
   *
   * @return the regex, or {@code null} when unset.
   */
  public @Nullable String getPattern() {
    return pattern;
  }

  /**
   * {@code @Pattern} message; set when {@link #getPattern()} comes from a {@code string} format.
   *
   * @return the message, or {@code null} when unset.
   */
  public @Nullable String getPatternMessage() {
    return patternMessage;
  }

  /**
   * {@code @Size} lower bound for a string.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable Integer getMinLength() {
    return minLength;
  }

  /**
   * {@code @Size} upper bound for a string.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable Integer getMaxLength() {
    return maxLength;
  }

  /**
   * Whether {@code format: "email"} was set, emitted as {@code @Email}.
   *
   * @return {@code true} when set.
   */
  public boolean isEmail() {
    return email;
  }

  /**
   * {@code @Min} bound for an {@code integer}.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable Long getMinimum() {
    return minimum;
  }

  /**
   * {@code @Max} bound for an {@code integer}.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable Long getMaximum() {
    return maximum;
  }

  /**
   * {@code @DecimalMin} bound for a {@code number} or a {@code format: "decimal"} string.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable String getDecimalMinimum() {
    return decimalMinimum;
  }

  /**
   * {@code @DecimalMax} bound for a {@code number} or a {@code format: "decimal"} string.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable String getDecimalMaximum() {
    return decimalMaximum;
  }

  /**
   * {@code @Size} lower bound for an array.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable Integer getMinItems() {
    return minItems;
  }

  /**
   * {@code @Size} upper bound for an array.
   *
   * @return the bound, or {@code null} when unset.
   */
  public @Nullable Integer getMaxItems() {
    return maxItems;
  }

  /**
   * Constraints applied to each element of an {@code array} property (from the facets inside its
   * {@code items} schema), emitted as container-element annotations on the field. Its own {@link
   * #getMinItems()} / {@link #getMaxItems()} and {@code #getElementConstraints()} are always unset.
   *
   * @return the element constraints, or {@code null} when the property is not an array or its
   *     {@code items} carried no facets.
   */
  public @Nullable Constraints getElementConstraints() {
    return elementConstraints;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Constraints other)) {
      return false;
    }
    return email == other.email
        && Objects.equals(pattern, other.pattern)
        && Objects.equals(patternMessage, other.patternMessage)
        && Objects.equals(minLength, other.minLength)
        && Objects.equals(maxLength, other.maxLength)
        && Objects.equals(minimum, other.minimum)
        && Objects.equals(maximum, other.maximum)
        && Objects.equals(decimalMinimum, other.decimalMinimum)
        && Objects.equals(decimalMaximum, other.decimalMaximum)
        && Objects.equals(minItems, other.minItems)
        && Objects.equals(maxItems, other.maxItems)
        && Objects.equals(elementConstraints, other.elementConstraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        pattern,
        patternMessage,
        minLength,
        maxLength,
        email,
        minimum,
        maximum,
        decimalMinimum,
        decimalMaximum,
        minItems,
        maxItems,
        elementConstraints);
  }

  @Override
  public String toString() {
    return "Constraints["
        + ("pattern=" + pattern)
        + (", patternMessage=" + patternMessage)
        + (", minLength=" + minLength)
        + (", maxLength=" + maxLength)
        + (", email=" + email)
        + (", minimum=" + minimum)
        + (", maximum=" + maximum)
        + (", decimalMinimum=" + decimalMinimum)
        + (", decimalMaximum=" + decimalMaximum)
        + (", minItems=" + minItems)
        + (", maxItems=" + maxItems)
        + (", elementConstraints=" + elementConstraints)
        + "]";
  }

  /** Accumulates {@link Constraints}; every flag starts unset. */
  public static final class Builder {

    private @Nullable String pattern = null;
    private @Nullable String patternMessage = null;
    private @Nullable Integer minLength = null;
    private @Nullable Integer maxLength = null;
    private boolean email = false;
    private @Nullable Long minimum = null;
    private @Nullable Long maximum = null;
    private @Nullable String decimalMinimum = null;
    private @Nullable String decimalMaximum = null;
    private @Nullable Integer minItems = null;
    private @Nullable Integer maxItems = null;
    private @Nullable Constraints elementConstraints = null;

    private Builder() {}

    /**
     * Sets the {@code @Pattern} regex.
     *
     * @param value regex from an explicit {@code pattern} or a {@code string} format.
     * @return {@code this}.
     */
    public Builder pattern(@Nullable String value) {
      this.pattern = value;
      return this;
    }

    /**
     * Sets the {@code @Pattern} message.
     *
     * @param value message used when {@code pattern} comes from a {@code string} format.
     * @return {@code this}.
     */
    public Builder patternMessage(@Nullable String value) {
      this.patternMessage = value;
      return this;
    }

    /**
     * Sets the {@code @Size} lower bound for a string.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder minLength(@Nullable Integer value) {
      this.minLength = value;
      return this;
    }

    /**
     * Sets the {@code @Size} upper bound for a string.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder maxLength(@Nullable Integer value) {
      this.maxLength = value;
      return this;
    }

    /**
     * Sets whether {@code format: "email"} was set, emitted as {@code @Email}.
     *
     * @param value {@code true} to set it.
     * @return {@code this}.
     */
    public Builder email(boolean value) {
      this.email = value;
      return this;
    }

    /**
     * Sets the {@code @Min} bound for an {@code integer}.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder minimum(@Nullable Long value) {
      this.minimum = value;
      return this;
    }

    /**
     * Sets the {@code @Max} bound for an {@code integer}.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder maximum(@Nullable Long value) {
      this.maximum = value;
      return this;
    }

    /**
     * Sets the {@code @DecimalMin} bound for a {@code number} or a {@code format: "decimal"}
     * string.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder decimalMinimum(@Nullable String value) {
      this.decimalMinimum = value;
      return this;
    }

    /**
     * Sets the {@code @DecimalMax} bound for a {@code number} or a {@code format: "decimal"}
     * string.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder decimalMaximum(@Nullable String value) {
      this.decimalMaximum = value;
      return this;
    }

    /**
     * Sets the {@code @Size} lower bound for an array.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder minItems(@Nullable Integer value) {
      this.minItems = value;
      return this;
    }

    /**
     * Sets the {@code @Size} upper bound for an array.
     *
     * @param value the bound.
     * @return {@code this}.
     */
    public Builder maxItems(@Nullable Integer value) {
      this.maxItems = value;
      return this;
    }

    /**
     * Sets the constraints applied to each element of an {@code array} property.
     *
     * @param value the element constraints, or {@code null} when unset.
     * @return {@code this}.
     */
    public Builder elementConstraints(@Nullable Constraints value) {
      this.elementConstraints = value;
      return this;
    }

    /**
     * Builds the constraint set from the accumulated fields.
     *
     * @return a new {@link Constraints}.
     */
    public Constraints build() {
      return new Constraints(
          pattern,
          patternMessage,
          minLength,
          maxLength,
          email,
          minimum,
          maximum,
          decimalMinimum,
          decimalMaximum,
          minItems,
          maxItems,
          elementConstraints);
    }

    @Override
    public String toString() {
      return "Constraints.Builder["
          + ("pattern=" + pattern)
          + (", patternMessage=" + patternMessage)
          + (", minLength=" + minLength)
          + (", maxLength=" + maxLength)
          + (", email=" + email)
          + (", minimum=" + minimum)
          + (", maximum=" + maximum)
          + (", decimalMinimum=" + decimalMinimum)
          + (", decimalMaximum=" + decimalMaximum)
          + (", minItems=" + minItems)
          + (", maxItems=" + maxItems)
          + (", elementConstraints=" + elementConstraints)
          + "]";
    }
  }
}
