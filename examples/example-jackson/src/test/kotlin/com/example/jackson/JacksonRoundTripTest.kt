package com.example.jackson

import com.example.dto.Address
import com.example.dto.LineItem
import com.example.dto.Manifest
import com.example.dto.Money
import com.example.dto.Order
import com.example.dto.Shipment
import com.example.dto.Tag
import com.example.dto.mutate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Serializes each generated model with Jackson 3's `JsonMapper` (jackson-module-kotlin registered),
 * checks the produced JSON, deserializes it back, and confirms the round trip is a different
 * instance with the same value.
 */
class JacksonRoundTripTest {

  private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

  @Test
  fun `simple type`() {
    val tag = Tag.builder().id("T1").label("urgent").build()

    val json = mapper.writeValueAsString(tag)
    val node = mapper.readTree(json)
    assertThat(node.get("id").asString()).isEqualTo("T1")
    assertThat(node.get("label").asString()).isEqualTo("urgent")

    val back = mapper.readValue(json, Tag::class.java)
    assertThat(back).isNotSameAs(tag).isEqualTo(tag)

    // modelmaker.kotlin.enabled: the generated Kotlin `mutate { }` extension over the builder.
    val relabeled = back.mutate { label("stale") }
    assertThat(relabeled.label).isEqualTo("stale")
    assertThat(back.label).isEqualTo("urgent")
  }

  @Test
  fun `type with many non-nullable params`() {
    val money = Money.builder().currency("USD").amount(19.99).scale(2).taxable(true).build()

    val json = mapper.writeValueAsString(money)
    val node = mapper.readTree(json)
    assertThat(node.get("currency").asString()).isEqualTo("USD")
    assertThat(node.get("amount").asDouble()).isEqualTo(19.99)
    assertThat(node.get("scale").asInt()).isEqualTo(2)
    assertThat(node.get("taxable").asBoolean()).isTrue()

    val back = mapper.readValue(json, Money::class.java)
    assertThat(back).isNotSameAs(money).isEqualTo(money)
  }

  @Test
  fun `type with nested object`() {
    val address = Address.builder().street("1 Main St").city("Springfield").build()
    val order = Order.builder().id("O1").shippingAddress(address).build()

    val json = mapper.writeValueAsString(order)
    val node = mapper.readTree(json)
    assertThat(node.get("shippingAddress").get("city").asString()).isEqualTo("Springfield")

    val back = mapper.readValue(json, Order::class.java)
    assertThat(back).isNotSameAs(order).isEqualTo(order)
    assertThat(back.shippingAddress).isNotSameAs(address).isEqualTo(address)
  }

  @Test
  fun `type with nested list of objects`() {
    val items =
        listOf(
            LineItem.builder().sku("A1").quantity(2).build(),
            LineItem.builder().sku("B2").quantity(1).build(),
        )
    val manifest = Manifest.builder().id("M1").items(items).build()

    val json = mapper.writeValueAsString(manifest)
    val node = mapper.readTree(json)
    assertThat(node.get("items")).hasSize(2)
    assertThat(node.get("items").get(1).get("sku").asString()).isEqualTo("B2")

    val back = mapper.readValue(json, Manifest::class.java)
    assertThat(back).isNotSameAs(manifest).isEqualTo(manifest)
    assertThat(back.items).isNotSameAs(items).isEqualTo(items)
  }

  @Test
  fun `enum-as-pattern field and long or float number formats`() {
    val shipment =
        Shipment.builder()
            .id("S1")
            .status("IN_TRANSIT")
            .distance(9_000_000_000L)
            .weight(72.5f)
            .build()

    val json = mapper.writeValueAsString(shipment)
    val node = mapper.readTree(json)
    assertThat(node.get("status").asString()).isEqualTo("IN_TRANSIT")
    // distance exceeds Int.MAX_VALUE - only representable because "format": "long" renders Long.
    assertThat(node.get("distance").asLong()).isEqualTo(9_000_000_000L)
    assertThat(node.get("weight").asDouble()).isEqualTo(72.5)

    val back = mapper.readValue(json, Shipment::class.java)
    assertThat(back).isNotSameAs(shipment).isEqualTo(shipment)
  }

  @Test
  fun `an unset optional enum field is filled in with its default`() {
    val shipment = Shipment.builder().id("S1").status("NEW").distance(10L).weight(1.0f).build()

    // "priority" was never set - "default": "NORMAL" is applied by the constructor.
    assertThat(shipment.priority).isEqualTo("NORMAL")

    val json = mapper.writeValueAsString(shipment)
    assertThat(mapper.readTree(json).get("priority").asString()).isEqualTo("NORMAL")

    val urgent = shipment.mutate { priority("HIGH") }
    assertThat(urgent.priority).isEqualTo("HIGH")
    assertThat(shipment.priority).isEqualTo("NORMAL")
  }
}
