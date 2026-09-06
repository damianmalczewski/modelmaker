package com.example.jackson2;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dto.Address;
import com.example.dto.LineItem;
import com.example.dto.Manifest;
import com.example.dto.Money;
import com.example.dto.Order;
import com.example.dto.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Serializes each generated model with Jackson 2's {@code ObjectMapper}, checks the produced JSON,
 * deserializes it back, and confirms the round trip is a different instance with the same value.
 */
class JacksonRoundTripTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void simpleType() throws Exception {
    Tag tag = Tag.builder().id("T1").label("urgent").build();

    String json = mapper.writeValueAsString(tag);
    JsonNode node = mapper.readTree(json);
    assertThat(node.get("id").asText()).isEqualTo("T1");
    assertThat(node.get("label").asText()).isEqualTo("urgent");
    // "priority" defaults to 0 and was never set: the constructor already applied the default, so
    // it serializes like any other value.
    assertThat(node.get("priority").asInt()).isEqualTo(0);

    Tag back = mapper.readValue(json, Tag.class);
    assertThat(back).isNotSameAs(tag).isEqualTo(tag);
  }

  @Test
  void aDefaultedFieldFallsBackWhenTheJsonOmitsItEntirely() throws Exception {
    // No "priority" key at all in the source JSON - the @JsonCreator constructor parameter comes
    // in as null, same as an unset builder field, and falls back to the schema default.
    Tag tag = mapper.readValue("{\"id\":\"T2\"}", Tag.class);

    assertThat(tag.getPriority()).isEqualTo(0);
  }

  @Test
  void typeWithManyNonNullableParams() throws Exception {
    Money money = Money.builder().currency("USD").amount(19.99).scale(2).taxable(true).build();

    String json = mapper.writeValueAsString(money);
    JsonNode node = mapper.readTree(json);
    assertThat(node.get("currency").asText()).isEqualTo("USD");
    assertThat(node.get("amount").asDouble()).isEqualTo(19.99);
    assertThat(node.get("scale").asInt()).isEqualTo(2);
    assertThat(node.get("taxable").asBoolean()).isTrue();

    Money back = mapper.readValue(json, Money.class);
    assertThat(back).isNotSameAs(money).isEqualTo(money);
  }

  @Test
  void typeWithNestedObject() throws Exception {
    Address address = Address.builder().street("1 Main St").city("Springfield").build();
    Order order = Order.builder().id("O1").shippingAddress(address).build();

    String json = mapper.writeValueAsString(order);
    JsonNode node = mapper.readTree(json);
    assertThat(node.get("shippingAddress").get("city").asText()).isEqualTo("Springfield");

    Order back = mapper.readValue(json, Order.class);
    assertThat(back).isNotSameAs(order).isEqualTo(order);
    assertThat(back.getShippingAddress()).isNotSameAs(address).isEqualTo(address);
  }

  @Test
  void typeWithNestedListOfObjects() throws Exception {
    List<LineItem> items =
        List.of(
            LineItem.builder().sku("A1").quantity(2).build(),
            LineItem.builder().sku("B2").quantity(1).build());
    Manifest manifest = Manifest.builder().id("M1").items(items).build();

    String json = mapper.writeValueAsString(manifest);
    JsonNode node = mapper.readTree(json);
    assertThat(node.get("items")).hasSize(2);
    assertThat(node.get("items").get(1).get("sku").asText()).isEqualTo("B2");

    Manifest back = mapper.readValue(json, Manifest.class);
    assertThat(back).isNotSameAs(manifest).isEqualTo(manifest);
    assertThat(back.getItems()).isNotSameAs(items).isEqualTo(items);
  }
}
