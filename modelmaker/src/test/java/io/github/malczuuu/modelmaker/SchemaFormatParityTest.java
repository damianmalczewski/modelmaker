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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The whole point of {@link AvroSchemaLoader} is that its output is indistinguishable from {@link
 * SimpleSchemaLoader}'s once it reaches {@link JavaModelMaker} - same immutable POJO, same
 * annotations, same Builder. This asserts that directly: two schemas describing the same shape, one
 * per format, must render to the exact same Java source.
 */
class SchemaFormatParityTest {

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
  void aJsonSchemaAndAnEquivalentAvroSchemaRenderToTheExactSameJava() {
    Path json =
        write(
            "x.Widget.json",
            """
            { "$modelmaker": "v1.0", "title": "x.Widget", "type": "object",
              "required": ["id", "count"],
              "properties": {
                "id": { "type": "string" },
                "count": { "type": "integer", "format": "long" },
                "label": { "type": "string" }
              }
            }
            """);
    Path avro =
        write(
            "x.Widget2.avsc",
            """
            { "type": "record", "namespace": "x", "name": "Widget2",
              "fields": [
                { "name": "id", "type": "string" },
                { "name": "count", "type": "long" },
                { "name": "label", "type": ["null", "string"], "default": null }
              ]
            }
            """);

    ModelType fromJson = new SimpleSchemaLoader().load(List.of(json)).get(0);
    ModelType fromAvro = new AvroSchemaLoader().load(List.of(avro)).get(0);

    ModelOptions options =
        ModelOptions.builder().jackson(true).validation(true).withers(true).build();
    String javaFromJson = new JavaModelMaker(options).emit(fromJson).replace("Widget", "Widget2");
    String javaFromAvro = new JavaModelMaker(options).emit(fromAvro);

    assertThat(javaFromAvro).isEqualTo(javaFromJson);
  }
}
