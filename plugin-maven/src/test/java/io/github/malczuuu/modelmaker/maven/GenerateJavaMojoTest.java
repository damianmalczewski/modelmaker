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

package io.github.malczuuu.modelmaker.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateJavaMojoTest {

  @TempDir Path projectDir;

  private Path schemaDir;
  private Path outputDir;
  private MavenProject project;

  @BeforeEach
  void beforeEach() {
    schemaDir = projectDir.resolve("src/main/model");
    outputDir = projectDir.resolve("target/generated-sources/modelmaker");
    project = new MavenProject();
  }

  @Test
  void generatesOneClassPerSchemaAndRegistersTheSourceRoot() throws Exception {
    schema(
        "com.example.dto.Person.json",
        """
        { "$modelmaker": "v1.0", "title": "com.example.dto.Person", "type": "object",
          "required": ["id"],
          "properties": { "id": { "type": "string" }, "age": { "type": "integer" } } }
        """);

    mojo(new Features()).execute();

    Path generated = outputDir.resolve("com/example/dto/Person.java");
    assertThat(generated).exists();
    assertThat(Files.readString(generated))
        .contains("public final class Person {")
        .doesNotContain("@JsonProperty");
    assertThat(project.getCompileSourceRoots()).contains(outputDir.toAbsolutePath().toString());
  }

  @Test
  void emitsTheAnnotationsOfTheEnabledFeatures() throws Exception {
    schema(
        "com.example.dto.Person.json",
        """
        { "$modelmaker": "v1.0", "title": "com.example.dto.Person", "type": "object",
          "required": ["id"],
          "properties": { "id": { "type": "string", "description": "The id." } } }
        """);

    Features features = new Features();
    features.getJackson().setEnabled(true);
    features.getValidation().setEnabled(true);
    features.getOpenApi().setEnabled(true);

    mojo(features).execute();

    assertThat(Files.readString(outputDir.resolve("com/example/dto/Person.java")))
        .contains("@JsonProperty(\"id\")")
        .contains("@NotNull(message = \"must not be null\")")
        .contains("@Schema(description = \"The id.\"");
  }

  @Test
  void toleratesAMissingSchemaDirectory() throws Exception {
    mojo(new Features()).execute();

    assertThat(outputDir).doesNotExist();
    assertThat(project.getCompileSourceRoots()).contains(outputDir.toAbsolutePath().toString());
  }

  @Test
  void wipesPreviouslyGeneratedSources() throws Exception {
    Files.createDirectories(outputDir.resolve("com/example/dto"));
    Path stale = outputDir.resolve("com/example/dto/Gone.java");
    Files.writeString(stale, "class Gone {}");
    schema(
        "com.example.dto.Person.json",
        """
        { "$modelmaker": "v1.0", "title": "com.example.dto.Person", "type": "object",
          "properties": { "id": { "type": "string" } } }
        """);

    mojo(new Features()).execute();

    assertThat(stale).doesNotExist();
    assertThat(outputDir.resolve("com/example/dto/Person.java")).exists();
  }

  @Test
  void reportsASchemaProblemAsABuildFailure() throws Exception {
    schema("com.example.dto.Person.json", "{ \"$modelmaker\": \"v1.0\", \"type\": \"object\" }");

    assertThatThrownBy(() -> mojo(new Features()).execute())
        .isInstanceOf(MojoExecutionException.class)
        .hasMessageContaining("missing \"title\"");
  }

  @Test
  void skipLeavesEverythingAlone() throws Exception {
    schema(
        "com.example.dto.Person.json",
        """
        { "$modelmaker": "v1.0", "title": "com.example.dto.Person", "type": "object",
          "properties": { "id": { "type": "string" } } }
        """);

    GenerateJavaMojo mojo = mojo(new Features());
    set(mojo, "skip", true);
    mojo.execute();

    assertThat(outputDir).doesNotExist();
    assertThat(project.getCompileSourceRoots())
        .doesNotContain(outputDir.toAbsolutePath().toString());
  }

  private GenerateJavaMojo mojo(Features features) {
    GenerateJavaMojo mojo = new GenerateJavaMojo();
    set(mojo, "schemaDirectory", schemaDir.toFile());
    set(mojo, "outputDirectory", outputDir.toFile());
    set(mojo, "skip", false);
    set(mojo, "features", features);
    set(mojo, "project", project);
    return mojo;
  }

  private void schema(String fileName, String content) {
    try {
      Files.createDirectories(schemaDir);
      Files.writeString(schemaDir.resolve(fileName), content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Mojo parameters are injected by Maven; the test sets them the same way, by field. */
  private static void set(GenerateJavaMojo mojo, String name, Object value) {
    try {
      Field field = GenerateJavaMojo.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(mojo, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("cannot set " + name, e);
    }
  }
}
