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

package io.github.malczuuu.modelmaker.gradle.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree

/** Extensions of the schema files both generation tasks read. */
internal val SCHEMA_INCLUDES: List<String> = listOf("**/*.json", "**/*.avsc")

/**
 * The schema files under [directory], the tracked input of both generation tasks.
 *
 * Filtering by extension is what keeps generated sources out of a task's own inputs: with
 * `modelmaker { src { enabled = true } }` they are written into a subdirectory of the schema
 * directory. The directories themselves must be left out of the fingerprint too (hence
 * `@IgnoreEmptyDirectories` on the properties built from this), or creating the output directory
 * would dirty the very task that created it.
 *
 * A directory that does not exist yields an empty tree rather than a validation failure, so
 * applying the plugin to a project without schemas is not an error.
 */
internal fun schemaFilesIn(directory: DirectoryProperty): FileTree =
    directory.asFileTree.matching { include(SCHEMA_INCLUDES) }
