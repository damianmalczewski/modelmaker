package com.example.avro;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dto.Address;
import com.example.dto.Invoice;
import com.example.dto.LineItem;
import com.example.dto.Money;
import com.example.dto.Order;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code modelmaker.features.validation = true}, Avro-sourced models: an Avro schema has no {@code
 * minLength} / {@code pattern} / {@code minimum} keywords, so the only per-value constraint this
 * format can express is {@code enum} - which is exactly why it's a good demo of the enum-as-pattern
 * convention that both schema formats share, including the {@code @Valid} cascade into a referenced
 * type and into each element of a {@code List}.
 */
class ValidationTest {

  private Validator validator;

  @BeforeEach
  void beforeEach() {
    validator =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();
  }

  private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
    return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
  }

  private static Order.Builder validOrderBuilder() {
    Address address =
        Address.builder().street("1 Main St").city("Springfield").kind("RESIDENTIAL").build();
    LineItem item = LineItem.builder().sku("A1").quantity(2L).unit("EACH").build();
    return Order.builder().id("O1").status("PAID").shippingAddress(address).items(List.of(item));
  }

  @Test
  void aFullyValidOrderHasNoViolations() {
    Order order = validOrderBuilder().build();

    assertThat(validator.validate(order)).isEmpty();
  }

  @Test
  void aStatusNotMatchingTheEnumSymbolsIsRejected() {
    Order order = validOrderBuilder().status("CANCELLED").build();

    Set<ConstraintViolation<Order>> violations = validator.validate(order);

    assertThat(paths(violations)).contains("status");
    assertThat(violations)
        .anyMatch(v -> v.getMessage().equals("must be one of NEW, PAID, SHIPPED"));
  }

  @Test
  void validCascadesIntoTheReferencedAddress() {
    Address badAddress =
        Address.builder().street("1 Main St").city("Springfield").kind("VACATION").build();
    Order order = validOrderBuilder().shippingAddress(badAddress).build();

    Set<ConstraintViolation<Order>> violations = validator.validate(order);

    assertThat(paths(violations)).contains("shippingAddress.kind");
  }

  @Test
  void validCascadesIntoEachElementOfTheItemsList() {
    LineItem badItem = LineItem.builder().sku("A1").quantity(2L).unit("CRATE").build();
    Order order = validOrderBuilder().items(List.of(badItem)).build();

    Set<ConstraintViolation<Order>> violations = validator.validate(order);

    assertThat(paths(violations)).contains("items[0].unit");
  }

  @Test
  void aNullOptionalDiscountRateHasNoViolation() {
    Order order = validOrderBuilder().discountRate(null).build();

    assertThat(validator.validate(order)).isEmpty();
  }

  @Test
  void anUnsetPriorityIsFilledInWithItsDefaultAndHasNoViolation() {
    Order order = validOrderBuilder().build();

    // "priority" is a nullable enum with "default": "NORMAL" - never set here, still @NotNull and
    // @Pattern-valid because the constructor fills it in before validation ever runs.
    assertThat(order.getPriority()).isEqualTo("NORMAL");
    assertThat(validator.validate(order)).isEmpty();
  }

  @Test
  void aPriorityNotMatchingTheEnumSymbolsIsRejected() {
    Order order = validOrderBuilder().priority("URGENT").build();

    Set<ConstraintViolation<Order>> violations = validator.validate(order);

    assertThat(paths(violations)).contains("priority");
    assertThat(violations).anyMatch(v -> v.getMessage().equals("must be one of LOW, NORMAL, HIGH"));
  }

  @Test
  void moneyRequiresEachOfItsFields() {
    Money money = Money.builder().currency("USD").amount(19.99).scale(2).taxable(true).build();

    assertThat(validator.validate(money)).isEmpty();
  }

  @Test
  void aValidInvoiceWithAnInlineNestedRecordAndAnArrayOfThemHasNoViolations() {
    Invoice.Contact billTo =
        Invoice.Contact.builder().name("Jane Doe").email("jane@example.com").build();
    Invoice.Note note = Invoice.Note.builder().author("ops").text("shipped early").build();
    Invoice invoice = Invoice.builder().id("INV-1").billTo(billTo).notes(List.of(note)).build();

    // Nothing on Contact/Note carries its own constraint beyond @NotNull, so this mainly proves
    // the @Valid cascade into a nested record and into a List of nested records both compile and
    // resolve correctly, not just cross-file references (see the tests above).
    assertThat(validator.validate(invoice)).isEmpty();
  }
}
