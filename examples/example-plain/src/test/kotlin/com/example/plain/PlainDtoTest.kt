package com.example.plain

import com.example.dto.Folder
import com.example.dto.Note
import com.example.dto.mutate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * No Jackson, no validation: plain immutable models with a builder, `mutate { }`, and value
 * semantics. `modelmaker.src.enabled = false` here, so the generated sources live under `build/`.
 *
 * Also showcases JSpecify: a required field (`Note.id`) is exposed to Kotlin as a non-null `String`
 * - no `?.`/`!!` needed to use it - while an optional one (`Note.pinned`, `Note.folder`) is exposed
 *   as a nullable type, forcing a null check at the call site. That's enforced by the Kotlin
 *   compiler at build time; the assertions below just confirm the runtime values match.
 *
 * And per-schema `"features"` overrides: `Note` sets `preferPrimitives`, so its always-set
 * `archived` flag is an `int`-style primitive `boolean` getter; `Folder` sets `withers`, so it gets
 * `withId` / `withName` single-field copy methods. Neither is on for the project.
 */
class PlainDtoTest {

  @Test
  fun `a required field is usable directly, with no null check`() {
    val note = Note.builder().id("N1").text("buy milk").build()

    // note.id is a non-null String per JSpecify: this line wouldn't compile otherwise.
    val idLength: Int = note.id.length
    assertThat(idLength).isEqualTo(2)
    assertThat(note.text).isEqualTo("buy milk")
  }

  @Test
  fun `an optional field is nullable and unset by default`() {
    val note = Note.builder().id("N1").text("buy milk").build()

    // note.pinned is a Boolean?, not a Boolean: it must be null-checked before use.
    assertThat(note.pinned).isNull()
    assertThat(note.folder).isNull()
  }

  @Test
  fun `a defaulted field is non-null - unlike a plain optional one, it needs no null check`() {
    val note = Note.builder().id("N1").text("buy milk").build()

    // note.archived is a Boolean, not a Boolean?: the constructor already applied the schema
    // default, so - unlike pinned above - no null check is needed to read it.
    val archived: Boolean = note.archived
    assertThat(archived).isFalse()

    // Passing null explicitly falls back to the default too, same as never setting it.
    val stillDefaulted = Note.builder().id("N1").text("buy milk").archived(null).build()
    assertThat(stillDefaulted.archived).isFalse()

    val archivedNote = note.mutate { archived(true) }
    assertThat(archivedNote.archived).isTrue()
    assertThat(note.archived).isFalse()
  }

  @Test
  fun `the builder rejects a missing required field`() {
    assertThatThrownBy { Note.builder().text("no id").build() }
        .isInstanceOf(NullPointerException::class.java)
  }

  @Test
  fun `mutate returns a new instance, the original is untouched`() {
    val note = Note.builder().id("N1").text("buy milk").build()

    val pinned = note.mutate { pinned(true) }

    assertThat(pinned.pinned).isTrue()
    assertThat(note.pinned).isNull()
    assertThat(pinned).isNotSameAs(note).isNotEqualTo(note)
  }

  @Test
  fun `a nested optional object is also nullable until set`() {
    val note = Note.builder().id("N1").text("buy milk").build()
    val folder = Folder.builder().id("F1").name("Inbox").build()

    val filed = note.mutate { folder(folder) }

    // filed.folder is a Folder?, so a safe call is required to reach its own non-null `name`.
    assertThat(filed.folder?.name).isEqualTo("Inbox")
    assertThat(note.folder).isNull()
  }

  @Test
  fun `Note preferPrimitives override - the always-set archived flag is a primitive boolean`() {
    // preferPrimitives on for this schema: `archived` (always set - it has a default) is a
    // primitive `boolean`, not the boxed `Boolean` a project default would give. The optional
    // `pinned` flag stays boxed - preferPrimitives only touches always-set scalars.
    assertThat(Note::class.java.getMethod("getArchived").returnType)
        .isEqualTo(Boolean::class.javaPrimitiveType)
    assertThat(Note::class.java.getMethod("getPinned").returnType)
        .isEqualTo(Boolean::class.javaObjectType)
  }

  @Test
  fun `Folder withers override - single-field copy methods are generated`() {
    val inbox = Folder.builder().id("F1").name("Inbox").build()

    val renamed = inbox.withName("Archive")

    assertThat(renamed.name).isEqualTo("Archive")
    assertThat(renamed.id).isEqualTo("F1")
    assertThat(inbox.name).isEqualTo("Inbox")
    assertThat(renamed).isNotSameAs(inbox)
  }

  @Test
  fun `two notes built with the same values are equal but not the same instance`() {
    val a = Note.builder().id("N1").text("buy milk").build()
    val b = Note.builder().id("N1").text("buy milk").build()

    assertThat(a).isNotSameAs(b).isEqualTo(b)
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
