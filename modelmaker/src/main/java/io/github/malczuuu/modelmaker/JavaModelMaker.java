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

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Emits an immutable Java POJO (final class, final fields, no setters, all-args constructor) for a
 * {@link ModelType}. The constructor is private, unless {@link ModelOptions#getJackson()} is on, in
 * which case it is package-private and carries {@code @JsonCreator}/{@code @JsonProperty} so
 * Jackson deserializes through it directly. Instances are also built through the nested {@code
 * Builder}. Inline objects become {@code static final} nested classes.
 */
public final class JavaModelMaker implements ModelMaker {

  private final ModelOptions baseOptions;
  private final String generatedAnnotation;

  /**
   * Creates a new {@link JavaModelMaker}.
   *
   * @param baseOptions the project's default {@link ModelOptions}, before any per-schema feature
   *     overrides are applied.
   */
  public JavaModelMaker(ModelOptions baseOptions) {
    this.baseOptions = Objects.requireNonNull(baseOptions, "baseOptions must not be null");
    this.generatedAnnotation = "@Generated(\"" + Constants.GENERATOR_NAME + "\")";
  }

  /**
   * Applies {@code type}'s feature overrides over the base options, then renders the whole file:
   * {@code package}, imports, the do-not-edit banner, and the top-level class (recursing into
   * nested types).
   *
   * @param type the schema-derived model to emit.
   * @return the generated file's full source text.
   */
  @Override
  public String emit(ModelType type) {
    Objects.requireNonNull(type, "type must not be null");

    ModelOptions options = type.getFeatureOverrides().applyOn(baseOptions);

    Set<String> imports = new TreeSet<>();
    imports.add("javax.annotation.processing.Generated");
    imports.add("org.jspecify.annotations.NullMarked");
    imports.add("org.jspecify.annotations.Nullable");
    imports.add("java.util.Objects");
    if (options.getJackson().isEnabled()) {
      imports.add("com.fasterxml.jackson.annotation.JsonIgnoreProperties");
      imports.add("com.fasterxml.jackson.annotation.JsonCreator");
      if (anyProperty(type)) {
        imports.add("com.fasterxml.jackson.annotation.JsonProperty");
      }
    }

    String body = renderClass(type, options, imports, "", true);

    StringBuilder out = new StringBuilder();
    out.append("package ").append(type.getPackageName()).append(";\n\n");
    for (String imp : imports) {
      out.append("import ").append(imp).append(";\n");
    }
    out.append('\n');
    out.append(Constants.GENERATED_FILE_NOTICE).append("\n\n");
    out.append(body);
    return out.toString();
  }

  private String renderClass(
      ModelType type, ModelOptions options, Set<String> imports, String indent, boolean topLevel) {
    StringBuilder b = new StringBuilder();
    b.append(indent).append(generatedAnnotation).append('\n');
    if (topLevel) {
      b.append(indent).append("@NullMarked\n");
    }
    if (options.getOpenApi().isEnabled()) {
      AnnotationSpec schema = OpenApiUtils.schemaAnnotationForType(type);
      if (schema != null) {
        imports.add(schema.getImportName());
        b.append(indent).append(schema.render()).append('\n');
      }
    }
    if (options.getJackson().isEnabled()) {
      b.append(indent).append("@JsonIgnoreProperties(ignoreUnknown = true)\n");
    }

    List<Property> props = type.getProperties();

    if (options.getJackson().isEnabled() && !props.isEmpty()) {
      imports.add("com.fasterxml.jackson.annotation.JsonPropertyOrder");
      b.append(indent)
          .append("@JsonPropertyOrder({")
          .append(props.stream().map(p -> "\"" + p.getJsonName() + "\"").collect(joining(", ")))
          .append("})\n");
    }
    b.append(indent).append(topLevel ? "public final" : "public static final");
    b.append(" class ").append(type.getName()).append(" {\n\n");

    String innerIndent = indent + "  ";
    RenderResult fields = new FieldsRenderer().init(innerIndent, type, options).render();
    imports.addAll(fields.getImports());
    b.append(fields.getCode());

    // All-args constructor (no-arg when the type has no properties). Package-private with
    // @JsonCreator/@JsonProperty when jackson is on, so Jackson deserializes directly through it
    // instead of through the Builder; otherwise private.
    RenderResult ctor = new ConstructorRenderer().init(innerIndent, type, options).render();
    imports.addAll(ctor.getImports());
    b.append(ctor.getCode()).append('\n');

    RenderResult getters = new GettersRenderer().init(innerIndent, type, options).render();
    imports.addAll(getters.getImports());
    b.append(getters.getCode());

    // `withXyz` methods
    if (options.isWithers()) {
      RenderResult withers = new WithersRenderer().init(innerIndent, type, options).render();
      imports.addAll(withers.getImports());
      b.append(withers.getCode());
    }

    RenderResult mutate = new MutateMethodRenderer().init(innerIndent, type, options).render();
    imports.addAll(mutate.getImports());
    b.append(mutate.getCode());

    RenderResult support = new SupportMethodsRenderer().init(innerIndent, type, options).render();
    imports.addAll(support.getImports());
    b.append(support.getCode());

    RenderResult builderFactory =
        new BuilderFactoryRenderer().init(innerIndent, type, options).render();
    imports.addAll(builderFactory.getImports());
    b.append('\n').append(builderFactory.getCode());

    RenderResult builder = new BuilderRenderer().init(innerIndent, type, options).render();
    imports.addAll(builder.getImports());
    b.append('\n').append(builder.getCode());

    for (ModelType nested : type.getNested()) {
      b.append('\n').append(renderClass(nested, options, imports, innerIndent, false));
    }

    RenderResult mutator = new BuilderMutatorRenderer().init(innerIndent, type, options).render();
    imports.addAll(mutator.getImports());
    b.append('\n').append(mutator.getCode());
    b.append(indent).append("}\n");
    return b.toString();
  }

  /**
   * Any type in the tree with at least one property - i.e. any {@code @JsonProperty} is emitted.
   *
   * @param type the model type to check, including its nested types.
   * @return {@code true} if {@code type} or any nested type has a property.
   */
  private boolean anyProperty(ModelType type) {
    return !type.getProperties().isEmpty() || type.getNested().stream().anyMatch(this::anyProperty);
  }
}
