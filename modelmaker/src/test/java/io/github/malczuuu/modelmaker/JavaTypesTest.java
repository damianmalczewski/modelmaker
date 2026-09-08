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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JavaTypesTest {

  private static final ModelOptions ALL_OFF = ModelOptions.builder().build();

  private static JavaTypes types(ModelOptions options) {
    return new JavaTypes("com.example.dto", options);
  }

  private final Set<String> imports = new HashSet<>();

  @AfterEach
  void beforeEach() {
    imports.clear();
  }

  @Test
  void boxedScalarsAreAlwaysTheWrapperType() {
    JavaTypes types = types(ModelOptions.builder().preferPrimitives(true).build());

    assertThat(types.boxed(PropType.ScalarType.STRING, imports)).isEqualTo("String");
    assertThat(types.boxed(PropType.ScalarType.INTEGER, imports)).isEqualTo("Integer");
    assertThat(types.boxed(PropType.ScalarType.NUMBER, imports)).isEqualTo("Double");
    assertThat(types.boxed(PropType.ScalarType.BOOLEAN, imports)).isEqualTo("Boolean");
    assertThat(imports).isEmpty();
  }

  @Test
  void nonNullScalarsBoxByDefault() {
    JavaTypes types = types(ALL_OFF);

    assertThat(types.nonNull(PropType.ScalarType.INTEGER, imports)).isEqualTo("Integer");
    assertThat(types.nonNull(PropType.ScalarType.NUMBER, imports)).isEqualTo("Double");
    assertThat(types.nonNull(PropType.ScalarType.BOOLEAN, imports)).isEqualTo("Boolean");
  }

  @Test
  void nonNullScalarsUsePrimitivesWhenPreferPrimitivesIsOn() {
    JavaTypes types = types(ModelOptions.builder().preferPrimitives(true).build());

    assertThat(types.nonNull(PropType.ScalarType.INTEGER, imports)).isEqualTo("int");
    assertThat(types.nonNull(PropType.ScalarType.NUMBER, imports)).isEqualTo("double");
    assertThat(types.nonNull(PropType.ScalarType.BOOLEAN, imports)).isEqualTo("boolean");
    assertThat(types.nonNull(PropType.ScalarType.STRING, imports)).isEqualTo("String");
  }

  @Test
  void bytesAlwaysRenderAsAByteArrayRegardlessOfBoxedOrPreferPrimitives() {
    JavaTypes plain = types(ALL_OFF);
    assertThat(plain.nonNull(PropType.ScalarType.BYTES, imports)).isEqualTo("byte[]");
    assertThat(plain.boxed(PropType.ScalarType.BYTES, imports)).isEqualTo("byte[]");

    JavaTypes primitives = types(ModelOptions.builder().preferPrimitives(true).build());
    assertThat(primitives.nonNull(PropType.ScalarType.BYTES, imports)).isEqualTo("byte[]");
    assertThat(imports).isEmpty();
  }

  @Test
  void aRefInTheTargetPackageIsStrippedToItsSimpleName() {
    String name = types(ALL_OFF).boxed(PropType.RefType.of("com.example.dto.Address"), imports);

    assertThat(name).isEqualTo("Address");
    assertThat(imports).isEmpty();
  }

  @Test
  void aNestedRefIsAlreadyAPlainName() {
    assertThat(types(ALL_OFF).boxed(PropType.RefType.of("Line"), imports)).isEqualTo("Line");
  }

  @Test
  void aRefInAnotherPackageIsLeftFullyQualified() {
    assertThat(types(ALL_OFF).boxed(PropType.RefType.of("com.other.Thing"), imports))
        .isEqualTo("com.other.Thing");
    assertThat(imports).isEmpty();
  }

  @Test
  void aJdkExternalTypeIsImportedAndUsedByItsSimpleName() {
    String name = types(ALL_OFF).boxed(PropType.ExternalType.of("java.time.Instant"), imports);

    assertThat(name).isEqualTo("Instant");
    assertThat(imports).containsExactly("java.time.Instant");
  }

  @Test
  void aJavaLangExternalTypeIsNotImported() {
    String name = types(ALL_OFF).boxed(PropType.ExternalType.of("java.lang.Object"), imports);

    assertThat(name).isEqualTo("Object");
    assertThat(imports).isEmpty();
  }

  @Test
  void anArrayOfScalarsIsAListAndImportsList() {
    String name = types(ALL_OFF).boxed(PropType.ArrayType.of(PropType.ScalarType.STRING), imports);

    assertThat(name).isEqualTo("List<String>");
    assertThat(imports).containsExactly("java.util.List");
  }

  @Test
  void aNestedArrayNests() {
    String name =
        types(ALL_OFF)
            .boxed(
                PropType.ArrayType.of(PropType.ArrayType.of(PropType.ScalarType.STRING)), imports);

    assertThat(name).isEqualTo("List<List<String>>");
  }

  @Test
  void anArrayOfRefsCascadesValidOnlyWhenValidationIsOn() {
    PropType type = PropType.ArrayType.of(PropType.RefType.of("com.example.dto.Address"));

    Set<String> off = new HashSet<>();
    assertThat(types(ALL_OFF).boxed(type, off)).isEqualTo("List<Address>");
    assertThat(off).containsExactly("java.util.List");

    Set<String> on = new HashSet<>();
    JavaTypes validating = types(ModelOptions.builder().validation(true).build());
    assertThat(validating.boxed(type, on)).isEqualTo("List<@Valid Address>");
    assertThat(on).containsExactlyInAnyOrder("java.util.List", "jakarta.validation.Valid");
  }

  @Test
  void anArrayOfExternalTypesImportsTheElement() {
    String name =
        types(ALL_OFF)
            .boxed(PropType.ArrayType.of(PropType.ExternalType.of("java.util.UUID")), imports);

    assertThat(name).isEqualTo("List<UUID>");
    assertThat(imports).containsExactlyInAnyOrder("java.util.List", "java.util.UUID");
  }

  @Test
  void equalWhenTargetPackageAndOptionsMatch() {
    JavaTypes a = new JavaTypes("com.example.dto", ALL_OFF);
    JavaTypes b = new JavaTypes("com.example.dto", ModelOptions.builder().build());

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void notEqualWhenTargetPackageDiffers() {
    assertThat(new JavaTypes("com.example.dto", ALL_OFF))
        .isNotEqualTo(new JavaTypes("com.other", ALL_OFF));
  }

  @Test
  void notEqualWhenOptionsDiffer() {
    assertThat(new JavaTypes("com.example.dto", ALL_OFF))
        .isNotEqualTo(types(ModelOptions.builder().preferPrimitives(true).build()));
  }

  @Test
  void notEqualToNullOrAnotherType() {
    JavaTypes types = types(ALL_OFF);

    assertThat(types).isNotEqualTo(null).isNotEqualTo("com.example.dto");
  }

  @Test
  void toStringReportsTargetPackageAndOptions() {
    assertThat(types(ALL_OFF).toString())
        .contains("JavaTypes[")
        .contains("targetPackage=com.example.dto")
        .contains("options=" + ALL_OFF);
  }
}
