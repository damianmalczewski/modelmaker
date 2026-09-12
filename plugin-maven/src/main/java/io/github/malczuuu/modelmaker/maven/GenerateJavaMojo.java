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

import io.github.malczuuu.modelmaker.JavaModelMaker;
import io.github.malczuuu.modelmaker.ModelType;
import io.github.malczuuu.modelmaker.SchemaException;
import io.github.malczuuu.modelmaker.SchemaLoaders;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Generates one immutable Java DTO per schema file found under {@code schemaDirectory}, and puts
 * the output directory on the project's compile source roots.
 *
 * <p>Bound to {@code generate-sources} by default, so {@code mvn compile} picks the generated types
 * up with no further setup. The output directory is wiped and rebuilt on every run - never edit
 * generated sources by hand.
 *
 * <p>Maven injects every {@code @Parameter} field before calling {@link #execute()}, which the
 * nullness analysis cannot see - hence the {@code NullAway.Init} suppressions on those fields.
 */
@Mojo(name = "generate-java", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateJavaMojo extends AbstractMojo {

  /** Creates a new {@link GenerateJavaMojo}. */
  public GenerateJavaMojo() {}

  /** Directory scanned recursively for schema files. Need not exist. */
  @Parameter(
      defaultValue = "${project.basedir}/src/main/model",
      property = "modelmaker.schemaDirectory")
  @SuppressWarnings("NullAway.Init")
  private File schemaDirectory;

  /** Where the generated sources are written; wiped before each run. */
  @Parameter(
      defaultValue = "${project.build.directory}/generated-sources/modelmaker",
      property = "modelmaker.outputDirectory")
  @SuppressWarnings("NullAway.Init")
  private File outputDirectory;

  /** Skips generation entirely. */
  @Parameter(defaultValue = "false", property = "modelmaker.skip")
  private boolean skip;

  /** Which features the generated code carries. */
  @Parameter private Features features = new Features();

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  @SuppressWarnings("NullAway.Init")
  private MavenProject project;

  /**
   * Loads every schema under {@code schemaDirectory} and writes the generated sources.
   *
   * @throws MojoExecutionException when a schema cannot be loaded, or the output cannot be written.
   */
  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("ModelMaker generation skipped");
      return;
    }

    Path outputDir = outputDirectory.toPath();
    deleteRecursively(outputDir);

    List<Path> schemas = schemaFiles();
    if (schemas.isEmpty()) {
      getLog().info("No model schemas found in " + schemaDirectory);
      registerSourceRoot();
      return;
    }

    JavaModelMaker javaModelMaker = new JavaModelMaker(features.toModelOptions());
    try {
      Files.createDirectories(outputDir);
      for (ModelType type : SchemaLoaders.createDelegatingSchemaLoader().load(schemas)) {
        write(
            outputDir, type.getPackageName(), type.getName() + ".java", javaModelMaker.emit(type));
        getLog().debug("Generated " + type.getPackageName() + "." + type.getName());
      }
    } catch (SchemaException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (IOException | UncheckedIOException e) {
      throw new MojoExecutionException("Failed to write generated sources to " + outputDir, e);
    }
    getLog().info("Generated " + schemas.size() + " model(s) into " + outputDir);

    registerSourceRoot();
  }

  private void registerSourceRoot() {
    project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
  }

  private List<Path> schemaFiles() throws MojoExecutionException {
    Path root = schemaDirectory.toPath();
    if (!Files.isDirectory(root)) {
      return List.of();
    }
    try (Stream<Path> files = Files.walk(root)) {
      return files
          .filter(Files::isRegularFile)
          .filter(GenerateJavaMojo::isSchemaFile)
          .sorted(Comparator.comparing(Path::toString))
          .toList();
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to scan " + root + " for schema files", e);
    }
  }

  private static boolean isSchemaFile(Path file) {
    String name = file.getFileName().toString();
    return name.endsWith(".json") || name.endsWith(".avsc");
  }

  private static void write(Path root, String packageName, String fileName, String code)
      throws IOException {
    Path dir = root.resolve(packageName.replace('.', '/'));
    Files.createDirectories(dir);
    Files.writeString(dir.resolve(fileName), code, StandardCharsets.UTF_8);
  }

  private static void deleteRecursively(Path directory) throws MojoExecutionException {
    if (!Files.exists(directory)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      List<Path> ordered = new ArrayList<>(paths.sorted(Comparator.reverseOrder()).toList());
      for (Path path : ordered) {
        Files.delete(path);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to clean " + directory, e);
    }
  }
}
