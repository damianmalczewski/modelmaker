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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Factory methods for the built-in {@link SchemaLoader}s. */
public final class SchemaLoaders {

  /**
   * Creates a {@link SchemaLoader} that dispatches by file extension: {@code *.json} to a {@link
   * SimpleSchemaLoader}, {@code *.avsc} to an {@link AvroSchemaLoader}. This is the loader a
   * build-tool integration (or any other caller with a mixed source directory) should use; {@link
   * SimpleSchemaLoader} and {@link AvroSchemaLoader} both stay usable standalone too.
   *
   * @return a new delegating {@link SchemaLoader}.
   */
  public static SchemaLoader createDelegatingSchemaLoader() {
    return new DelegatingSchemaLoader(new SimpleSchemaLoader(), new AvroSchemaLoader());
  }

  private SchemaLoaders() {}

  /**
   * {@link SchemaLoader} that routes each file to the delegate whose {@link
   * SchemaLoader#canLoad(String)} accepts its extension. Each format resolves its own named-type
   * references only within its own file set - there is no cross-format reference resolution.
   *
   * <p>Package-private; created through {@link SchemaLoaders#createDelegatingSchemaLoader()}.
   */
  static final class DelegatingSchemaLoader implements SchemaLoader {

    private final List<SchemaLoader> loaders;

    /**
     * Creates a new {@link DelegatingSchemaLoader}, delegating to the given loaders, tried in
     * order.
     *
     * @param loaders the delegates; the first one whose {@link SchemaLoader#canLoad(String)}
     *     accepts a file's extension parses it.
     */
    DelegatingSchemaLoader(SchemaLoader... loaders) {
      this.loaders = List.of(loaders);
    }

    /**
     * Accepts an extension when any delegate does.
     *
     * @param extension a file extension, passed through to each delegate as-is.
     * @return {@code true} when any delegate's {@link SchemaLoader#canLoad(String)} accepts it.
     */
    @Override
    public boolean canLoad(String extension) {
      return loaders.stream().anyMatch(loader -> loader.canLoad(extension));
    }

    /**
     * Splits {@code files} by extension and delegates each subset to the matching loader.
     *
     * @param files schema files to parse, {@code *.json} and/or {@code *.avsc}.
     * @return every delegate's {@link ModelType}s, in delegate order.
     */
    @Override
    public List<ModelType> load(List<Path> files) {
      Objects.requireNonNull(files, "files must not be null");

      List<List<Path>> byLoader = new ArrayList<>();
      for (int i = 0; i < loaders.size(); i++) {
        byLoader.add(new ArrayList<>());
      }
      for (Path file : files) {
        String extension = extension(file);
        int index = indexOfLoaderFor(file, extension);
        byLoader.get(index).add(file);
      }

      List<ModelType> types = new ArrayList<>();
      for (int i = 0; i < loaders.size(); i++) {
        types.addAll(loaders.get(i).load(byLoader.get(i)));
      }
      return List.copyOf(types);
    }

    private int indexOfLoaderFor(Path file, String extension) {
      for (int i = 0; i < loaders.size(); i++) {
        if (loaders.get(i).canLoad(extension)) {
          return i;
        }
      }
      throw new IllegalArgumentException(
          file.getFileName() + ": unsupported file extension \"" + extension + "\"");
    }
  }

  private static String extension(Path file) {
    String name = file.getFileName().toString();
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
