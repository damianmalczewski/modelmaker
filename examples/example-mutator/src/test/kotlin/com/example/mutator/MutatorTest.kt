package com.example.mutator

import com.example.dto.Kit
import com.example.dto.Widget
import com.example.dto.mutate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * `modelmaker.kotlin.enabled = true`: the generated `mutate { }` extension takes a `Type.Mutator.()
 * -> Unit` block, not `Type.Builder.() -> Unit`. This exercises the ordinary behavior (functional)
 * and the structural guarantee behind why `build()` can't be called from inside the block:
 * `Mutator` never declares it, and every `Mutator` method returns `Mutator`, so even a chained call
 * inside the block stays on `Mutator` - it can't be written here, since a `note.mutate { build() }`
 * line simply fails to compile. The reflection checks below assert that guarantee at the bytecode
 * level instead, as a regression test for the code generator.
 */
class MutatorTest {

  @Test
  fun `mutate applies the block and leaves the original untouched`() {
    val widget = Widget.builder().id("W1").name("Widget").build()

    val renamed = widget.mutate { name("Renamed") }

    assertThat(renamed.name).isEqualTo("Renamed")
    assertThat(widget.name).isEqualTo("Widget")
    assertThat(renamed).isNotSameAs(widget).isNotEqualTo(widget)
  }

  @Test
  fun `mutate works on a nested type too`() {
    val widget = Widget.builder().id("W1").name("Widget").build()
    val kit = Kit.builder().id("K1").widget(widget).build()

    val rewidgeted = kit.mutate { widget(widget.mutate { name("New name") }) }

    assertThat(rewidgeted.widget.name).isEqualTo("New name")
    assertThat(kit.widget.name).isEqualTo("Widget")
  }

  @Test
  fun `Builder implements Mutator`() {
    assertThat(Widget.BuilderMutator::class.java.isAssignableFrom(Widget.Builder::class.java))
        .isTrue()
  }

  @Test
  fun `Mutator declares no build method`() {
    assertThat(Widget.BuilderMutator::class.java.methods.map { it.name }).doesNotContain("build")
  }

  @Test
  fun `every Mutator setter returns Mutator, so chaining never reaches Builder`() {
    val setterNames = setOf("id", "name", "note")

    Widget.BuilderMutator::class
        .java
        .methods
        .filter { it.name in setterNames }
        .forEach { method ->
          assertThat(method.returnType)
              .`as`("return type of Mutator.%s", method.name)
              .isEqualTo(Widget.BuilderMutator::class.java)
        }
  }

  @Test
  fun `Builder's own setters still return Builder, so Java chaining is unaffected`() {
    val setterNames = setOf("id", "name", "note")

    // The compiler also emits a synthetic bridge method per covariant override (return type
    // Mutator, to satisfy the interface's erased signature) - skip those, they aren't the real one.
    Widget.Builder::class
        .java
        .declaredMethods
        .filter { it.name in setterNames && !it.isBridge }
        .forEach { method ->
          assertThat(method.returnType)
              .`as`("return type of Builder.%s", method.name)
              .isEqualTo(Widget.Builder::class.java)
        }
  }
}
