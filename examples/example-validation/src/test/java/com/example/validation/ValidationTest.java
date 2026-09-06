package com.example.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dto.Address;
import com.example.dto.LineItem;
import com.example.dto.Manifest;
import com.example.dto.Order;
import com.example.dto.Person;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code modelmaker.annotations.validation = true}, no Jackson: exercises the generated
 * `jakarta.validation` annotations against a real {@link Validator} backed by Hibernate Validator.
 *
 * <p>Every generated constraint message is already a hardcoded literal string (see the plugin's
 * "Validation mapping" doc), never an EL expression, so {@link ParameterMessageInterpolator} is
 * used to interpolate messages without needing a {@code jakarta.el} implementation on the
 * classpath.
 */
class ValidationTest {

  private Validator validator;

  @BeforeEach
  void beforeEach() {
    try (ValidatorFactory factory =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
    return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
  }

  @Test
  void aFullyValidPersonHasNoViolations() {
    Person person = Person.builder().id("P1").email("a@b.com").age(30).bio("hi").build();

    assertThat(validator.validate(person)).isEmpty();
  }

  @Test
  void anIdNotMatchingThePatternIsRejected() {
    Person person = Person.builder().id("nope").email("a@b.com").build();

    assertThat(paths(validator.validate(person))).contains("id");
  }

  @Test
  void aMalformedEmailIsRejected() {
    Person person = Person.builder().id("P1").email("not-an-email").build();

    assertThat(paths(validator.validate(person))).contains("email");
  }

  @Test
  void ageOutsideMinAndMaxIsRejected() {
    Person tooYoung = Person.builder().id("P1").email("a@b.com").age(-1).build();
    Person tooOld = Person.builder().id("P1").email("a@b.com").age(200).build();

    assertThat(paths(validator.validate(tooYoung))).contains("age");
    assertThat(paths(validator.validate(tooOld))).contains("age");
  }

  @Test
  void aBioLongerThanMaxLengthIsRejected() {
    Person person = Person.builder().id("P1").email("a@b.com").bio("x".repeat(201)).build();

    assertThat(paths(validator.validate(person))).contains("bio");
  }

  @Test
  void anUnsetDefaultedFieldIsFilledInBeforeValidationEverRuns() {
    Person person = Person.builder().id("P1").email("a@b.com").build();

    // "active" defaults to true; it's already applied by build(), not something the validator
    // itself does, so there's no violation either way - just confirming the value round trips.
    assertThat(person.getActive()).isTrue();
    assertThat(validator.validate(person)).isEmpty();

    Person inactive = person.mutate().active(false).build();
    assertThat(inactive.getActive()).isFalse();
  }

  @Test
  void validCascadesIntoTheNestedAddress() {
    Address blank = Address.builder().street("").city("").build();
    Order order = Order.builder().id("O1").shippingAddress(blank).build();

    Set<ConstraintViolation<Order>> violations = validator.validate(order);

    assertThat(paths(violations)).contains("shippingAddress.street", "shippingAddress.city");
  }

  @Test
  void validCascadesIntoEachElementOfTheItemsList() {
    LineItem bad = LineItem.builder().sku("not valid").quantity(0).build();
    Manifest manifest = Manifest.builder().id("M1").items(List.of(bad)).build();

    Set<ConstraintViolation<Manifest>> violations = validator.validate(manifest);

    assertThat(paths(violations)).contains("items[0].sku", "items[0].quantity");
  }

  @Test
  void anEmptyItemsListViolatesTheMinItemsSize() {
    Manifest manifest = Manifest.builder().id("M1").items(List.of()).build();

    assertThat(paths(validator.validate(manifest))).contains("items");
  }
}
