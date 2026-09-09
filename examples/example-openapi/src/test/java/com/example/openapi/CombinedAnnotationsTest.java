package com.example.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dto.Address;
import com.example.dto.Customer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code modelmaker { features { jackson = true; validation = true; openapi = true } }}: the three
 * annotation sets coexist on one generated DTO. Jackson binds it, Hibernate Validator enforces the
 * constraints, and the OpenAPI {@code @Schema} metadata is readable via reflection.
 */
class CombinedAnnotationsTest {

  private final ObjectMapper mapper = new ObjectMapper();

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

  private static Customer valid() {
    return Customer.builder()
        .id("C42")
        .email("ada@example.com")
        .shippingAddress(Address.builder().city("Kraków").postalCode("30-001").build())
        .build();
  }

  @Test
  void jacksonRoundTripsThroughTheNestedAddress() throws Exception {
    Customer customer = valid();

    String json = mapper.writeValueAsString(customer);
    JsonNode node = mapper.readTree(json);
    assertThat(node.get("id").asText()).isEqualTo("C42");
    assertThat(node.get("shippingAddress").get("postalCode").asText()).isEqualTo("30-001");
    // "loyaltyPoints" defaults to 0 and was never set
    assertThat(node.get("loyaltyPoints").asInt()).isEqualTo(0);

    Customer back = mapper.readValue(json, Customer.class);
    assertThat(back).isNotSameAs(customer).isEqualTo(customer);
  }

  @Test
  void aFullyValidCustomerHasNoViolations() {
    assertThat(validator.validate(valid())).isEmpty();
  }

  @Test
  void patternAndEmailConstraintsAreEnforced() {
    Customer bad =
        Customer.builder()
            .id("nope")
            .email("not-an-email")
            .shippingAddress(Address.builder().city("X").postalCode("30-001").build())
            .build();

    assertThat(paths(validator.validate(bad))).contains("id", "email");
  }

  @Test
  void validCascadesIntoTheShippingAddress() {
    Customer customer =
        Customer.builder()
            .id("C1")
            .email("a@b.com")
            .shippingAddress(Address.builder().city("").postalCode("bad").build())
            .build();

    assertThat(paths(validator.validate(customer)))
        .contains("shippingAddress.city", "shippingAddress.postalCode");
  }

  @Test
  void loyaltyPointsOutsideTheRangeIsRejected() {
    Customer customer = valid().mutate().loyaltyPoints(-1).build();

    assertThat(paths(validator.validate(customer))).contains("loyaltyPoints");
  }

  @Test
  void theTypeCarriesAnOpenApiSchemaDescription() {
    Schema schema = Customer.class.getAnnotation(Schema.class);

    assertThat(schema).isNotNull();
    assertThat(schema.description()).startsWith("Combines Jackson binding");
  }

  @Test
  void aDocumentedRequiredFieldCarriesDescriptionExampleAndRequiredMode() throws Exception {
    Schema schema = Customer.class.getDeclaredField("id").getAnnotation(Schema.class);

    assertThat(schema).isNotNull();
    assertThat(schema.description()).isEqualTo("Customer identifier, C followed by digits.");
    assertThat(schema.example()).isEqualTo("C42");
    assertThat(schema.requiredMode()).isEqualTo(Schema.RequiredMode.REQUIRED);
  }

  @Test
  void aDefaultedOptionalFieldIsNotMarkedRequired() throws Exception {
    Schema schema = Customer.class.getDeclaredField("loyaltyPoints").getAnnotation(Schema.class);

    assertThat(schema).isNotNull();
    assertThat(schema.description()).isEqualTo("Accrued loyalty points.");
    assertThat(schema.requiredMode()).isEqualTo(Schema.RequiredMode.AUTO);
  }
}
