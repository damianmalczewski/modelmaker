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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DelegatingSchemaLoaderTest {

  private final SchemaLoader loader = SchemaLoaders.createDelegatingSchemaLoader();

  @TempDir Path dir;

  private Path write(String name, String content) {
    Path f = dir.resolve(name);
    try {
      Files.writeString(f, content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return f;
  }

  @Test
  void mergesJsonAndAvroFilesEachParsedByTheMatchingLoader() {
    Path json =
        write(
            "x.Widget.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Widget\", \"type\": \"object\","
                + " \"properties\": { \"id\": { \"type\": \"string\" } } }");
    Path avro =
        write(
            "x.Gadget.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Gadget\", \"fields\": ["
                + " { \"name\": \"id\", \"type\": \"string\" } ] }");

    List<ModelType> types = loader.load(List.of(json, avro));

    assertThat(types.stream().map(ModelType::getName))
        .containsExactlyInAnyOrder("Widget", "Gadget");
  }

  @Test
  void canLoadIsTrueForJsonAndAvscAndFalseOtherwise() {
    assertThat(loader.canLoad("json")).isTrue();
    assertThat(loader.canLoad("avsc")).isTrue();
    assertThat(loader.canLoad("yaml")).isFalse();
  }

  @Test
  void canLoadWithANullExtensionIsFalse() {
    assertThat(loader.canLoad(null)).isFalse();
  }

  @Test
  void rejectsAnUnsupportedFileExtension() {
    Path f = write("x.Bad.yaml", "irrelevant");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("unsupported file extension \"yaml\"");
  }

  @Test
  void rejectsAFileWithNoExtensionAtAll() {
    Path f = write("Widget", "irrelevant");
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("unsupported file extension \"\"");
  }

  @Test
  void loadRejectsANullFileList() {
    assertThatNullPointerException().isThrownBy(() -> loader.load(null));
  }

  @Test
  void loadWithAnEmptyFileListReturnsAnEmptyList() {
    assertThat(loader.load(List.of())).isEmpty();
  }

  @Test
  void aBatchOfOnlyJsonFilesWorksWithoutAnyAvscFiles() {
    Path json =
        write(
            "x.Widget.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Widget\", \"type\": \"object\","
                + " \"properties\": { \"id\": { \"type\": \"string\" } } }");

    assertThat(loader.load(List.of(json)).stream().map(ModelType::getName))
        .containsExactly("Widget");
  }

  @Test
  void aBatchOfOnlyAvscFilesWorksWithoutAnyJsonFiles() {
    Path avro =
        write(
            "x.Gadget.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Gadget\", \"fields\": ["
                + " { \"name\": \"id\", \"type\": \"string\" } ] }");

    assertThat(loader.load(List.of(avro)).stream().map(ModelType::getName))
        .containsExactly("Gadget");
  }

  @Test
  void resultOrderFollowsDelegateOrderNotInputFileOrder() {
    Path avro =
        write(
            "x.Gadget.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Gadget\", \"fields\": ["
                + " { \"name\": \"id\", \"type\": \"string\" } ] }");
    Path json =
        write(
            "x.Widget.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Widget\", \"type\": \"object\","
                + " \"properties\": { \"id\": { \"type\": \"string\" } } }");

    // avro is listed first in the input, but the default delegate order is [json, avro] - proves
    // the output order tracks delegate registration, not the caller's file list order.
    List<ModelType> types = loader.load(List.of(avro, json));

    assertThat(types.stream().map(ModelType::getName)).containsExactly("Widget", "Gadget");
  }

  @Test
  void rejectsWithoutCorruptingStateWhenOneFileInABatchHasAnUnsupportedExtension() {
    Path good1 =
        write(
            "x.Widget.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Widget\", \"type\": \"object\","
                + " \"properties\": { \"id\": { \"type\": \"string\" } } }");
    Path bad = write("x.Bad.yaml", "irrelevant");
    Path good2 =
        write(
            "x.Gadget.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Gadget\", \"fields\": ["
                + " { \"name\": \"id\", \"type\": \"string\" } ] }");

    assertThatThrownBy(() -> loader.load(List.of(good1, bad, good2)))
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("unsupported file extension \"yaml\"");

    // retrying without the bad file still works, proving the rejected call left no shared state
    // behind on this stateless loader.
    assertThat(loader.load(List.of(good1, good2)).stream().map(ModelType::getName))
        .containsExactlyInAnyOrder("Widget", "Gadget");
  }

  @Test
  void dispatchIsCaseInsensitiveEvenThoughTheDelegateItselfIsCaseSensitiveAboutFilenames() {
    Path f =
        write(
            "x.Widget.JSON",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Widget\", \"type\": \"object\","
                + " \"properties\": { \"id\": { \"type\": \"string\" } } }");

    // ".JSON" is routed to SimpleSchemaLoader (dispatch is case-insensitive), which then rejects
    // it for its own, unrelated reason: the filename must match the title's case exactly. A
    // dispatch-level rejection would instead say "unsupported file extension".
    assertThatThrownBy(() -> loader.load(List.of(f)))
        .hasMessageContaining("file must be named \"x.Widget.json\"")
        .satisfies(e -> assertThat(e).hasMessageNotContaining("unsupported file extension"));
  }

  @Test
  void theSameTypeNameFromBothFormatsProducesTwoSeparateModelTypesNoCollisionCheck() {
    Path json =
        write(
            "x.Widget.json",
            "{ \"$modelmaker\": \"v1.0\", \"title\": \"x.Widget\", \"type\": \"object\","
                + " \"properties\": { \"id\": { \"type\": \"string\" } } }");
    Path avro =
        write(
            "x.Widget.avsc",
            "{ \"type\": \"record\", \"namespace\": \"x\", \"name\": \"Widget\", \"fields\": ["
                + " { \"name\": \"id\", \"type\": \"string\" } ] }");

    List<ModelType> types = loader.load(List.of(json, avro));

    assertThat(types).hasSize(2);
    assertThat(types).allMatch(t -> t.getName().equals("Widget"));
  }

  @Test
  void reportsUnsupportedExtensionsAndSchemaProblemsTogether() {
    Path unsupported = write("x.A.yaml", "title: x.A");
    Path broken = write("x.B.json", "{ \"$modelmaker\": \"v1.0\", \"type\": \"object\" }");

    assertThatThrownBy(() -> loader.load(List.of(unsupported, broken)))
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("2 schema errors:")
        .hasMessageContaining("x.A.yaml: unsupported file extension \"yaml\"")
        .hasMessageContaining("x.B.json: missing \"title\"");
  }
}
