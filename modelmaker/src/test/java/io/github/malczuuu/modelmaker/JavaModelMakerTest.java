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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class JavaModelMakerTest {

  /** Baseline options: withers, Jackson and validation on, primitives off - tweak per test. */
  private static ModelOptions.Builder opts() {
    return ModelOptions.builder().withers(true).jackson(true).validation(true);
  }

  @Test
  void emitIsIndependentPerCallEvenWhenSchemasDisagreeOnFeatures() {
    ModelType withoutJackson =
        new ModelType(
            "Plain",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "id", PropType.ScalarType.STRING, true, Constraints.none(), "id", null)),
            List.of(),
            FeatureOverrides.builder().jackson(JacksonOverride.enabled(false)).build());

    JavaModelMaker maker = new JavaModelMaker(opts().build());

    String before = maker.emit(address);
    String plain = maker.emit(withoutJackson);
    String after = maker.emit(address);

    assertThat(plain).doesNotContain("@JsonCreator");
    assertThat(after).isEqualTo(before).contains("@JsonCreator");
  }

  @Test
  void emitIsSafeToCallConcurrentlyOnOneInstance() throws Exception {
    JavaModelMaker maker = new JavaModelMaker(opts().build());
    List<ModelType> types = List.of(address, person, withNested);
    List<String> expected = types.stream().map(maker::emit).toList();

    int rounds = 50;
    ExecutorService executor = Executors.newFixedThreadPool(types.size());
    try {
      List<Callable<Boolean>> calls = new ArrayList<>();
      for (int round = 0; round < rounds; round++) {
        for (int i = 0; i < types.size(); i++) {
          int index = i;
          calls.add(() -> maker.emit(types.get(index)).equals(expected.get(index)));
        }
      }
      for (Future<Boolean> result : executor.invokeAll(calls)) {
        assertThat(result.get()).isTrue();
      }
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void constructorRejectsANullBaseOptions() {
    assertThatNullPointerException().isThrownBy(() -> new JavaModelMaker(null));
  }

  @Test
  void emitRejectsANullType() {
    assertThatNullPointerException()
        .isThrownBy(() -> new JavaModelMaker(opts().build()).emit(null));
  }

  private final ModelType address =
      new ModelType(
          "Address",
          "com.example.dto",
          "An address.",
          List.of(
              new Property(
                  "city", PropType.ScalarType.STRING, true, Constraints.none(), "city", null)),
          List.of(),
          FeatureOverrides.none());

  private final ModelType person =
      new ModelType(
          "Person",
          "com.example.dto",
          null,
          List.of(
              new Property(
                  "id",
                  PropType.ScalarType.STRING,
                  true,
                  Constraints.builder().pattern("^X\\d+$").build(),
                  "id",
                  null),
              new Property(
                  "age",
                  PropType.ScalarType.INTEGER,
                  false,
                  Constraints.builder().minimum(0L).maximum(120L).build(),
                  "age",
                  null),
              new Property(
                  "home", PropType.RefType.of("Address"), true, Constraints.none(), "home", null),
              new Property(
                  "tags",
                  PropType.ArrayType.of(PropType.ScalarType.STRING),
                  false,
                  Constraints.builder().maxItems(5).build(),
                  "tags",
                  null)),
          List.of(),
          FeatureOverrides.none());

  private final ModelType withNested =
      new ModelType(
          "Order",
          "com.example.dto",
          null,
          List.of(
              new Property(
                  "ref", PropType.RefType.of("Address"), true, Constraints.none(), "ref", null),
              new Property(
                  "line", PropType.RefType.of("Line"), true, Constraints.none(), "line", null)),
          List.of(
              new ModelType(
                  "Line",
                  "",
                  "An inline nested object.",
                  List.of(
                      new Property(
                          "sku",
                          PropType.ScalarType.STRING,
                          true,
                          Constraints.none(),
                          "sku",
                          null)),
                  List.of(),
                  FeatureOverrides.none())),
          FeatureOverrides.none());

  private final ModelType flag =
      new ModelType(
          "Flag",
          "x",
          null,
          List.of(
              new Property("on", PropType.ScalarType.BOOLEAN, true, Constraints.none(), "on", null),
              new Property(
                  "count", PropType.ScalarType.INTEGER, true, Constraints.none(), "count", null)),
          List.of(),
          FeatureOverrides.none());

  @Test
  void javaImmutablePojo() {
    String out = new JavaModelMaker(opts().build()).emit(person);

    assertThat(out).contains("@NullMarked");
    assertThat(out).contains("@JsonPropertyOrder({\"id\", \"age\", \"home\", \"tags\"})");
    assertThat(out).contains("public final class Person {");
    assertThat(out).contains("private final String id;");
    assertThat(out).contains("private final @Nullable Integer age;");
    assertThat(out).contains("private final Address home;");
    assertThat(out).contains("private final @Nullable List<String> tags;");
    assertThat(out).contains("@JsonCreator");
    assertThat(out).contains("  @JsonCreator\n  Person(\n      @JsonProperty(\"id\") String id,\n");
    assertThat(out).contains("public @Nullable Integer getAge() {");
    assertThat(out).contains("public boolean equals(@Nullable Object obj) {");
    assertThat(out).contains("return Objects.hash(id, age, home, tags);");
    assertThat(out).contains("public String toString() {");
    assertThat(out).contains("\"Person[\"");
    assertThat(out).contains("+ \"]\"");
    assertThat(out)
        .contains("@Pattern(regexp = \"^X\\\\d+$\", message = \"must match \\\"^X\\\\d+$\\\"\")");
    assertThat(out).contains("@Min(value = 0, message = \"must be greater than or equal to 0\")");
    assertThat(out).contains("@Valid");
  }

  @Test
  void javaMarksGeneratedFilesWithABannerAndNoAnnotation() {
    String out =
        new JavaModelMaker(opts().jackson(false).validation(false).build()).emit(withNested);

    // No @Generated: it would force a modular consumer to `requires java.compiler`.
    assertThat(out)
        .doesNotContain("javax.annotation.processing.Generated")
        .doesNotContain("@Generated");
    assertThat(out)
        .contains(
            ";\n\n// Generated by ModelMaker (io.github.malczuuu.modelmaker). Do not edit"
                + " manually.\n\n@NullMarked\npublic final class Order {");
    assertThat(out).contains("\n  public static final class Line {");
    assertThat(out).contains("\n  public static final class Builder implements BuilderMutator {");
    assertThat(out).contains("\n  public sealed interface BuilderMutator permits Builder {");
  }

  @Test
  void javaEmitsJsonIncludeOnlyWhenIncludeNonNullIsOn() {
    String withoutIt = new JavaModelMaker(opts().build()).emit(person);

    assertThat(withoutIt).doesNotContain("@JsonInclude");

    String withIt =
        new JavaModelMaker(
                opts()
                    .jackson(JacksonConfig.builder().enabled(true).includeNonNull(true).build())
                    .build())
            .emit(person);

    assertThat(withIt)
        .contains("import com.fasterxml.jackson.annotation.JsonInclude;")
        .contains(
            "@JsonIgnoreProperties(ignoreUnknown = true)\n"
                + "@JsonInclude(JsonInclude.Include.NON_NULL)");
  }

  @Test
  void javaEmitsJsonIncludeOnNestedTypesToo() {
    String out =
        new JavaModelMaker(
                opts()
                    .jackson(JacksonConfig.builder().enabled(true).includeNonNull(true).build())
                    .build())
            .emit(withNested);

    assertThat(out)
        .contains(
            "@JsonInclude(JsonInclude.Include.NON_NULL)\n"
                + "  @JsonPropertyOrder({\"sku\"})\n"
                + "  public static final class Line {");
  }

  @Test
  void javaSkipsJsonIncludeWhenJacksonItselfIsOff() {
    String out =
        new JavaModelMaker(
                opts()
                    .jackson(JacksonConfig.builder().enabled(false).includeNonNull(true).build())
                    .build())
            .emit(person);

    assertThat(out).doesNotContain("@JsonInclude");
  }

  @Test
  void javaEmitsWithXyzCopyMethods() {
    String out = new JavaModelMaker(opts().build()).emit(person);
    assertThat(out).contains("public Person withId(String id) {");
    assertThat(out).contains("public Person withAge(@Nullable Integer age) {");
    assertThat(out)
        .contains("return new Person(Objects.requireNonNull(id), this.age, this.home, this.tags);");
    assertThat(out).contains("return new Person(this.id, age, this.home, this.tags);");
  }

  @Test
  void withersOffOmitsWithXyzMethodsButKeepsMutateAndTheBuilder() {
    String out = new JavaModelMaker(opts().withers(false).build()).emit(person);
    assertThat(out).doesNotContain("withId");
    assertThat(out).doesNotContain("withAge");
    assertThat(out).contains("public Builder mutate() {");
    assertThat(out).contains("public static Builder builder() {");
  }

  @Test
  void javaEmitsInlineObjectsAsStaticNestedClasses() {
    String out = new JavaModelMaker(opts().build()).emit(withNested);
    assertThat(out).contains("public final class Order {");
    assertThat(out).contains("  public static final class Line {");
    assertThat(out).contains("private final Address ref;");
    assertThat(out).contains("private final Line line;");
    assertThat(out).containsOnlyOnce("@NullMarked");
  }

  @Test
  void javaRendersAJdkRefAsItsSimpleNameWithAnImportNoValid() {
    ModelType type =
        new ModelType(
            "TimeRange",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "since",
                    PropType.ExternalType.of("java.time.Instant"),
                    true,
                    Constraints.none(),
                    "since",
                    null),
                new Property(
                    "ids",
                    PropType.ArrayType.of(PropType.ExternalType.of("java.util.UUID")),
                    false,
                    Constraints.none(),
                    "ids",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().build()).emit(type);
    assertThat(out).contains("import java.time.Instant;");
    assertThat(out).contains("import java.util.UUID;");
    assertThat(out).contains("private final Instant since;");
    assertThat(out).contains("private final @Nullable List<UUID> ids;");
    assertThat(out).doesNotContain("@Valid");
    assertThat(out).contains("Objects.requireNonNull(since, \"since is required\")");
  }

  @Test
  void javaCascadesValidationIntoAListOfDtosViaTheElementType() {
    ModelType type =
        new ModelType(
            "Bag",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "home", PropType.RefType.of("Address"), true, Constraints.none(), "home", null),
                new Property(
                    "extras",
                    PropType.ArrayType.of(PropType.RefType.of("Address")),
                    false,
                    Constraints.builder().maxItems(5).build(),
                    "extras",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().build()).emit(type);
    // default placement is getters: the field stays plain, the getter carries @Valid
    assertThat(out).contains("private final Address home;");
    assertThat(out).contains("@Valid\n  public Address getHome() {");
    assertThat(out).contains("private final @Nullable List<Address> extras;");
    assertThat(out).contains("public @Nullable List<@Valid Address> getExtras() {");
    assertThat(out).contains("import jakarta.validation.Valid;");

    String plain = new JavaModelMaker(opts().validation(false).build()).emit(type);
    assertThat(plain).doesNotContain("@Valid");
    assertThat(plain).doesNotContain("jakarta.validation");
    assertThat(plain).contains("private final @Nullable List<Address> extras;");
  }

  @Test
  void javaPutsElementConstraintsOnTheListGetterByDefault() {
    ModelType type =
        new ModelType(
            "CreateDeviceRequest",
            "com.example.rest",
            null,
            List.of(
                new Property(
                    "tags",
                    PropType.ArrayType.of(PropType.ScalarType.STRING),
                    false,
                    Constraints.builder()
                        .elementConstraints(
                            Constraints.builder().minLength(1).maxLength(255).build())
                        .build(),
                    "tags",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().build()).emit(type);

    assertThat(out)
        .contains(
            "public @Nullable List<@Size(min = 1, max = 255, message = \"size must be"
                + " between 1 and 255\") String> getTags() {");
    assertThat(out).contains("import jakarta.validation.constraints.Size;");
    // field, constructor param and builder keep the plain List<String>
    assertThat(out).contains("private final @Nullable List<String> tags;");
    assertThat(out).containsOnlyOnce("List<@Size");
  }

  @Test
  void javaGettersReturnAnUnmodifiableViewOfACollectionField() {
    ModelType type =
        new ModelType(
            "Bag",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "tags",
                    PropType.ArrayType.of(PropType.ScalarType.STRING),
                    true,
                    Constraints.none(),
                    "tags",
                    null),
                new Property(
                    "notes",
                    PropType.ArrayType.of(PropType.ScalarType.STRING),
                    false,
                    Constraints.none(),
                    "notes",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().build()).emit(type);

    assertThat(out).contains("import java.util.Collections;");
    assertThat(out).contains("    return Collections.unmodifiableList(tags);\n");
    assertThat(out)
        .contains("    return notes != null ? Collections.unmodifiableList(notes) : null;\n");
  }

  @Test
  void jacksonAnnotationsNeedsNoJacksonDatabindPackageOnlyJacksonAnnotations() {
    String out = new JavaModelMaker(opts().validation(false).build()).emit(person);
    assertThat(out).contains("import com.fasterxml.jackson.annotation.JsonCreator;");
    assertThat(out).contains("import com.fasterxml.jackson.annotation.JsonProperty;");
    assertThat(out).doesNotContain("databind");
  }

  private final ModelType documented =
      new ModelType(
          "Account",
          "com.example.dto",
          "A billing account.",
          List.of(
              new Property(
                  "id",
                  PropType.ScalarType.STRING,
                  true,
                  Constraints.none(),
                  "id",
                  null,
                  "the account identifier",
                  "A-42"),
              new Property(
                  "nickname",
                  PropType.ScalarType.STRING,
                  false,
                  Constraints.none(),
                  "nickname",
                  null,
                  null,
                  null)),
          List.of(),
          FeatureOverrides.none());

  @Test
  void openApiEmitsSchemaOnTheClassAndOnDocumentedFieldsOnly() {
    String out =
        new JavaModelMaker(opts().jackson(false).validation(false).openApi(true).build())
            .emit(documented);

    assertThat(out).contains("import io.swagger.v3.oas.annotations.media.Schema;");
    assertThat(out).contains("@Schema(description = \"A billing account.\")");
    assertThat(out)
        .contains(
            "@Schema(description = \"the account identifier\", example = \"A-42\","
                + " requiredMode = Schema.RequiredMode.REQUIRED)");
    // the undocumented "nickname" field gets no @Schema
    assertThat(out).doesNotContain("@Schema(description = \"\")");
  }

  @Test
  void openApiOffEmitsNoSchemaAnnotations() {
    String out =
        new JavaModelMaker(opts().jackson(false).validation(false).openApi(false).build())
            .emit(documented);

    assertThat(out).doesNotContain("@Schema");
    assertThat(out).doesNotContain("io.swagger");
  }

  @Test
  void aSchemasOpenApiOverrideWinsOverTheProjectDefaultInBothDirections() {
    JavaModelMaker offByDefault = new JavaModelMaker(opts().openApi(false).build());
    JavaModelMaker onByDefault = new JavaModelMaker(opts().openApi(true).build());

    assertThat(
            offByDefault.emit(
                documented.withFeatureOverrides(
                    FeatureOverrides.builder().openApi(OpenApiOverride.enabled(true)).build())))
        .contains("@Schema(description = \"A billing account.\")");
    assertThat(
            onByDefault.emit(
                documented.withFeatureOverrides(
                    FeatureOverrides.builder().openApi(OpenApiOverride.enabled(false)).build())))
        .doesNotContain("@Schema");
  }

  @Test
  void emitsAPlainModelWithNeitherJacksonNorValidationWhenBothAreOff() {
    String out = new JavaModelMaker(opts().jackson(false).validation(false).build()).emit(person);

    assertThat(out).doesNotContain("jackson");
    assertThat(out).doesNotContain("jakarta");
    assertThat(out).doesNotContain("@JsonProperty");
    assertThat(out).doesNotContain("@JsonCreator");
    assertThat(out).doesNotContain("@NotNull");
    assertThat(out).doesNotContain("@Valid");
    assertThat(out).contains("@NullMarked");
    assertThat(out).contains("public final class Person {");
    assertThat(out).contains("private Person(\n");
    assertThat(out).contains("public static Builder builder() {");
    assertThat(out).contains("public Person build() {");
    assertThat(out).contains("Objects.requireNonNull(id, \"id is required\")");
  }

  @Test
  void aSchemasJacksonOverrideWinsOverTheProjectDefaultInBothDirections() {
    JavaModelMaker onByDefault = new JavaModelMaker(opts().jackson(true).build());
    JavaModelMaker offByDefault = new JavaModelMaker(opts().jackson(false).build());

    String forcedOff =
        onByDefault.emit(
            person.withFeatureOverrides(
                FeatureOverrides.builder().jackson(JacksonOverride.enabled(false)).build()));
    assertThat(forcedOff).doesNotContain("@JsonCreator");

    String forcedOn =
        offByDefault.emit(
            person.withFeatureOverrides(
                FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build()));
    assertThat(forcedOn).contains("@JsonCreator");
  }

  @Test
  void aSchemasValidationOverrideWinsOverTheProjectDefaultInBothDirections() {
    JavaModelMaker onByDefault = new JavaModelMaker(opts().validation(true).build());
    JavaModelMaker offByDefault = new JavaModelMaker(opts().validation(false).build());

    String forcedOff =
        onByDefault.emit(
            person.withFeatureOverrides(
                FeatureOverrides.builder().validation(ValidationOverride.enabled(false)).build()));
    assertThat(forcedOff).doesNotContain("@NotNull");

    String forcedOn =
        offByDefault.emit(
            person.withFeatureOverrides(
                FeatureOverrides.builder().validation(ValidationOverride.enabled(true)).build()));
    assertThat(forcedOn).contains("@NotNull");
  }

  @Test
  void aSchemasWithersOverrideWinsOverTheProjectDefaultInBothDirections() {
    JavaModelMaker onByDefault = new JavaModelMaker(opts().withers(true).build());
    JavaModelMaker offByDefault = new JavaModelMaker(opts().withers(false).build());

    String forcedOff =
        onByDefault.emit(
            person.withFeatureOverrides(FeatureOverrides.builder().withers(false).build()));
    assertThat(forcedOff).doesNotContain("withId");

    String forcedOn =
        offByDefault.emit(
            person.withFeatureOverrides(FeatureOverrides.builder().withers(true).build()));
    assertThat(forcedOn).contains("public Person withId(String id) {");
  }

  @Test
  void aSchemasPreferPrimitivesOverrideWinsOverTheProjectDefaultInBothDirections() {
    JavaModelMaker onByDefault = new JavaModelMaker(opts().preferPrimitives(true).build());
    JavaModelMaker offByDefault = new JavaModelMaker(opts().preferPrimitives(false).build());

    String forcedOff =
        onByDefault.emit(
            flag.withFeatureOverrides(FeatureOverrides.builder().preferPrimitives(false).build()));
    assertThat(forcedOff).contains("private final Boolean on;");

    String forcedOn =
        offByDefault.emit(
            flag.withFeatureOverrides(FeatureOverrides.builder().preferPrimitives(true).build()));
    assertThat(forcedOn).contains("private final boolean on;");
  }

  @Test
  void withNoOverrideTheProjectDefaultAppliesAsBefore() {
    String out = new JavaModelMaker(opts().jackson(true).build()).emit(person);
    assertThat(out).contains("@JsonCreator");
  }

  @Test
  void anOverrideOnOneSchemaDoesNotLeakIntoTheNextEmitCall() {
    JavaModelMaker emitter = new JavaModelMaker(opts().jackson(false).build());

    String overridden =
        emitter.emit(
            person.withFeatureOverrides(
                FeatureOverrides.builder().jackson(JacksonOverride.enabled(true)).build()));
    assertThat(overridden).contains("@JsonCreator");

    String next = emitter.emit(address);
    assertThat(next).doesNotContain("@JsonCreator");
  }

  @Test
  void fieldUsesTheCamelCaseNameJsonPropertyKeepsTheJsonKey() {
    ModelType type =
        new ModelType(
            "Row",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "userId",
                    PropType.ScalarType.STRING,
                    true,
                    Constraints.none(),
                    "user_id",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String java = new JavaModelMaker(opts().build()).emit(type);
    assertThat(java).contains("private final String userId;");
    assertThat(java).contains("public String getUserId() {");
    assertThat(java).contains("@JsonPropertyOrder({\"user_id\"})");
    assertThat(java).contains("@JsonProperty(\"user_id\")");
    assertThat(java).contains("@JsonProperty(\"user_id\") String userId) {");
    assertThat(java).doesNotContain("@JsonProperty(\"user_id\")\n    public Builder");
  }

  @Test
  void javaBoxesRequiredScalarsByDefault() {
    String out = new JavaModelMaker(opts().build()).emit(flag);
    assertThat(out).contains("private final Boolean on;");
    assertThat(out).contains("private final Integer count;");
    assertThat(out).contains("public Boolean getOn() {");
    assertThat(out).doesNotContain("private final boolean");
  }

  @Test
  void javaUsesPrimitivesForRequiredScalarsWhenPreferPrimitivesIsOn() {
    String out = new JavaModelMaker(opts().preferPrimitives(true).build()).emit(flag);
    assertThat(out).contains("private final boolean on;");
    assertThat(out).contains("private final int count;");
    assertThat(out)
        .contains(
            "  @JsonCreator\n  Flag(\n      @JsonProperty(\"on\") boolean on,\n     "
                + " @JsonProperty(\"count\") int count) {");
    assertThat(out).contains("public boolean getOn() {");

    assertThat(new JavaModelMaker(opts().preferPrimitives(true).build()).emit(address))
        .contains("private final String city;");
  }

  @Test
  void javaNestedBuilder() {
    String java = new JavaModelMaker(opts().build()).emit(person);
    assertThat(java).contains("public static Builder builder() {");
    assertThat(java).contains("public static final class Builder implements BuilderMutator {");
    assertThat(java).contains("private Builder() {}");
    assertThat(java).contains("private @Nullable String id;");
    assertThat(java).contains("@Override\n    public Builder id(@Nullable String id) {");
    assertThat(java).doesNotContain("@JsonProperty(\"id\")\n    public Builder");
    assertThat(java).contains("public Person build() {");
    assertThat(java).contains("Objects.requireNonNull(id, \"id is required\")");
    assertThat(java).contains("\"Person.Builder[\"");
    assertThat(java).contains("public Builder mutate() {");
    assertThat(java).contains("return builder()");
    assertThat(java).contains(".id(id)");
  }

  @Test
  void javaNestedMutatorInterfaceExposesSettersButNotBuild() {
    String java = new JavaModelMaker(opts().build()).emit(person);
    assertThat(java).contains("public sealed interface BuilderMutator permits Builder {");
    assertThat(java).contains("Mutator id(@Nullable String id);");
    assertThat(java).contains("Mutator age(@Nullable Integer age);");

    String marker = "public sealed interface BuilderMutator permits Builder {";
    String after = java.substring(java.indexOf(marker) + marker.length());
    String mutatorBody = after.substring(0, after.indexOf("\n  }"));
    assertThat(mutatorBody).doesNotContain("build(");
  }

  @Test
  void aPropertyLessTypeStillGetsANoArgCtorAndABuilder() {
    ModelType empty =
        new ModelType(
            "Empty",
            "com.example.dto",
            "An empty type.",
            List.of(),
            List.of(),
            FeatureOverrides.none());
    String java = new JavaModelMaker(opts().build()).emit(empty);

    assertThat(java).doesNotContain("/**");
    assertThat(java).contains("@JsonCreator\n  Empty() {}");
    assertThat(java).contains("public static Builder builder() {");
    assertThat(java).contains("public static final class Builder implements BuilderMutator {");
    assertThat(java).contains("public Empty build() {\n      return new Empty();\n");
    assertThat(java).contains("public Builder mutate() {\n    return builder();\n");
    assertThat(java).doesNotContain("@JsonPropertyOrder");
    assertThat(java).contains("\"Empty[]\"");
  }

  @Test
  void builderAppliesADefault() {
    ModelType type =
        new ModelType(
            "S",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "currency",
                    PropType.ScalarType.STRING,
                    false,
                    Constraints.none(),
                    "currency",
                    DefaultValue.Str.of("USD"))),
            List.of(),
            FeatureOverrides.none());

    String java = new JavaModelMaker(opts().build()).emit(type);
    assertThat(java).contains("public S build() {");
    assertThat(java).contains("return new S(");
    assertThat(java).doesNotContain("require(currency");
  }

  @Test
  void defaultValues() {
    ModelType type =
        new ModelType(
            "Settings",
            "com.example.dto",
            null,
            List.of(
                new Property(
                    "active",
                    PropType.ScalarType.BOOLEAN,
                    false,
                    Constraints.none(),
                    "active",
                    DefaultValue.Bool.of(true)),
                new Property(
                    "currency",
                    PropType.ScalarType.STRING,
                    false,
                    Constraints.none(),
                    "currency",
                    DefaultValue.Str.of("USD")),
                new Property(
                    "roles",
                    PropType.ArrayType.of(PropType.ScalarType.STRING),
                    false,
                    Constraints.none(),
                    "roles",
                    DefaultValue.Arr.of(List.of(DefaultValue.Str.of("user"))))),
            List.of(),
            FeatureOverrides.none());

    String java = new JavaModelMaker(opts().build()).emit(type);
    assertThat(java).contains("private final Boolean active;");
    assertThat(java).contains("private final String currency;");
    assertThat(java).contains("private final List<String> roles;");
    assertThat(java).contains("      @JsonProperty(\"active\") @Nullable Boolean active,");
    assertThat(java).contains("this.active = active != null ? active : true;");
    assertThat(java).contains("this.currency = currency != null ? currency : \"USD\";");
    assertThat(java).contains("this.roles = roles != null ? roles : List.of(\"user\");");
    assertThat(java).contains("public Boolean getActive() {");
    assertThat(java).contains("public Settings withActive(Boolean active) {");

    String prim = new JavaModelMaker(opts().preferPrimitives(true).build()).emit(type);
    assertThat(prim).contains("private final boolean active;");
    assertThat(prim).contains("      @JsonProperty(\"active\") @Nullable Boolean active,");
    assertThat(prim).contains("this.active = active != null ? active : true;");
    assertThat(prim).contains("public boolean getActive() {");
    assertThat(prim).contains("public Settings withActive(boolean active) {");
  }

  @Test
  void longAndFloatRenderAsTheirOwnBoxedOrPrimitiveTypeNotIntOrDouble() {
    ModelType type =
        new ModelType(
            "Wide",
            "x",
            null,
            List.of(
                new Property(
                    "big",
                    PropType.ScalarType.LONG,
                    true,
                    Constraints.none(),
                    "big",
                    DefaultValue.Num.of("42")),
                new Property(
                    "small",
                    PropType.ScalarType.FLOAT,
                    true,
                    Constraints.none(),
                    "small",
                    DefaultValue.Num.of("3.5"))),
            List.of(),
            FeatureOverrides.none());

    String boxed = new JavaModelMaker(opts().build()).emit(type);
    assertThat(boxed).contains("private final Long big;");
    assertThat(boxed).contains("private final Float small;");
    assertThat(boxed).contains("this.big = big != null ? big : 42L;");
    assertThat(boxed).contains("this.small = small != null ? small : 3.5f;");

    String prim = new JavaModelMaker(opts().preferPrimitives(true).build()).emit(type);
    assertThat(prim).contains("private final long big;");
    assertThat(prim).contains("private final float small;");
  }

  @Test
  void enumRendersAsAStringFieldWithAPatternNeverAJavaEnum() {
    ModelType type =
        new ModelType(
            "Order",
            "x",
            null,
            List.of(
                new Property(
                    "status",
                    PropType.ScalarType.STRING,
                    true,
                    Constraints.builder()
                        .pattern("^(\\QNEW\\E|\\QPAID\\E)$")
                        .patternMessage("must be one of NEW, PAID")
                        .build(),
                    "status",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().build()).emit(type);

    assertThat(out).contains("private final String status;");
    assertThat(out)
        .contains(
            "@Pattern(regexp = \"^(\\\\QNEW\\\\E|\\\\QPAID\\\\E)$\", "
                + "message = \"must be one of NEW, PAID\")");
    assertThat(out).doesNotContain("enum Status");
    assertThat(out).doesNotContain("public enum");
  }

  @Test
  void bytesRenderAsAByteArrayFieldWithDefensiveCopiesEverywhere() {
    ModelType type =
        new ModelType(
            "Blob",
            "x",
            null,
            List.of(
                new Property(
                    "payload",
                    PropType.ScalarType.BYTES,
                    true,
                    Constraints.none(),
                    "payload",
                    null),
                new Property(
                    "signature",
                    PropType.ScalarType.BYTES,
                    false,
                    Constraints.none(),
                    "signature",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().build()).emit(type);

    assertThat(out).contains("import java.util.Arrays;");
    assertThat(out).contains("import java.util.Base64;");
    assertThat(out).contains("private final byte[] payload;");
    assertThat(out).contains("private final @Nullable byte[] signature;");
    // constructor: stores the array raw - the copy is the builder's / wither's job
    assertThat(out).contains("this.payload = payload;");
    assertThat(out).contains("this.signature = signature;");
    // getters: defensive copy out
    assertThat(out).contains("public byte[] getPayload() {\n    return payload.clone();\n  }");
    assertThat(out)
        .contains(
            "public @Nullable byte[] getSignature() {\n"
                + "    return signature != null ? signature.clone() : null;\n"
                + "  }");
    // wither: the caller's replacement array is copied
    assertThat(out)
        .contains("return new Blob(Objects.requireNonNull(payload).clone(), this.signature);");
    assertThat(out)
        .contains("return new Blob(this.payload, signature != null ? signature.clone() : null);");
    // builder: defensive copy in
    assertThat(out).contains("this.payload = payload != null ? payload.clone() : null;");
    // equals / hashCode compare by content; toString prints base64
    assertThat(out).contains("Arrays.equals(payload, other.payload)");
    assertThat(out).contains("+ \"payload=\" + Base64.getEncoder().encodeToString(payload)");
    assertThat(out)
        .contains(
            "+ \", signature=\""
                + " + (signature != null ? Base64.getEncoder().encodeToString(signature) :"
                + " \"null\")");
    assertThat(out).contains("Objects.hash(Arrays.hashCode(payload), Arrays.hashCode(signature));");
  }

  @Test
  void bytesUseAByteArrayEvenWhenPreferPrimitivesIsOn() {
    ModelType type =
        new ModelType(
            "Blob",
            "x",
            null,
            List.of(
                new Property(
                    "payload",
                    PropType.ScalarType.BYTES,
                    true,
                    Constraints.none(),
                    "payload",
                    null)),
            List.of(),
            FeatureOverrides.none());

    String out = new JavaModelMaker(opts().preferPrimitives(true).build()).emit(type);

    assertThat(out).contains("private final byte[] payload;");
  }
}
