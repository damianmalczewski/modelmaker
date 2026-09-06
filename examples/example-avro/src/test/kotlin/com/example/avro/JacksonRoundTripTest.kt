package com.example.avro

import com.example.dto.Address
import com.example.dto.Invoice
import com.example.dto.LineItem
import com.example.dto.Money
import com.example.dto.Order
import com.example.dto.mutate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Models generated from `.avsc` (Avro) schemas are indistinguishable at this layer from ones
 * generated from the plain JSON schema format - same Jackson round trip, same `mutate { }`
 * extension, see [com.example.avro.ValidationTest] for the `jakarta.validation` side of the same
 * story.
 */
class JacksonRoundTripTest {

  private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

  @Test
  fun `enum-as-pattern field round trips as a plain string`() {
    val address =
        Address.builder().street("1 Main St").city("Springfield").kind("RESIDENTIAL").build()
    val item = LineItem.builder().sku("A1").quantity(2L).unit("EACH").build()
    val order =
        Order.builder().id("O1").status("PAID").shippingAddress(address).items(listOf(item)).build()

    val json = mapper.writeValueAsString(order)
    val node = mapper.readTree(json)
    assertThat(node.get("status").asString()).isEqualTo("PAID")
    assertThat(node.get("shippingAddress").get("city").asString()).isEqualTo("Springfield")
    assertThat(node.get("shippingAddress").get("kind").asString()).isEqualTo("RESIDENTIAL")
    assertThat(node.get("items").get(0).get("quantity").asLong()).isEqualTo(2L)
    assertThat(node.get("discountRate").isNull).isTrue()

    val back = mapper.readValue(json, Order::class.java)
    assertThat(back).isNotSameAs(order).isEqualTo(order)

    // modelmaker.kotlin.enabled: the generated Kotlin `mutate { }` extension over the builder.
    val discounted = back.mutate { discountRate(0.1f) }
    assertThat(discounted.discountRate).isEqualTo(0.1f)
    assertThat(back.discountRate).isNull()
  }

  @Test
  fun `a nullable enum field left unset is filled in with its non-null default`() {
    val address =
        Address.builder().street("1 Main St").city("Springfield").kind("RESIDENTIAL").build()
    val item = LineItem.builder().sku("A1").quantity(2L).unit("EACH").build()
    val order =
        Order.builder().id("O1").status("PAID").shippingAddress(address).items(listOf(item)).build()

    // "priority" was never set - "default": "NORMAL" on the enum's nullable union is applied by
    // the constructor, not something the caller has to do.
    assertThat(order.priority).isEqualTo("NORMAL")

    val json = mapper.writeValueAsString(order)
    assertThat(mapper.readTree(json).get("priority").asString()).isEqualTo("NORMAL")

    val urgent = order.mutate { priority("HIGH") }
    assertThat(urgent.priority).isEqualTo("HIGH")
    assertThat(order.priority).isEqualTo("NORMAL")
  }

  @Test
  fun `long, double, int and boolean each round trip as their own distinct type`() {
    val money = Money.builder().currency("USD").amount(19.99).scale(2).taxable(true).build()

    val json = mapper.writeValueAsString(money)
    val node = mapper.readTree(json)
    assertThat(node.get("amount").asDouble()).isEqualTo(19.99)
    assertThat(node.get("scale").asInt()).isEqualTo(2)
    assertThat(node.get("taxable").asBoolean()).isTrue()

    val back = mapper.readValue(json, Money::class.java)
    assertThat(back).isNotSameAs(money).isEqualTo(money)
  }

  @Test
  fun `an inline nested record and an array of inline nested records round trip`() {
    val billTo = Invoice.Contact.builder().name("Jane Doe").email("jane@example.com").build()
    val notes =
        listOf(
            Invoice.Note.builder().author("ops").text("shipped early").build(),
            Invoice.Note.builder().author("billing").text("net 30").build(),
        )
    val invoice = Invoice.builder().id("INV-1").billTo(billTo).notes(notes).build()

    val json = mapper.writeValueAsString(invoice)
    val node = mapper.readTree(json)
    assertThat(node.get("billTo").get("email").asString()).isEqualTo("jane@example.com")
    assertThat(node.get("notes")).hasSize(2)
    assertThat(node.get("notes").get(1).get("author").asString()).isEqualTo("billing")

    val back = mapper.readValue(json, Invoice::class.java)
    assertThat(back).isNotSameAs(invoice).isEqualTo(invoice)
    assertThat(back.billTo).isNotSameAs(billTo).isEqualTo(billTo)
    assertThat(back.notes).isNotSameAs(notes).isEqualTo(notes)
  }
}
