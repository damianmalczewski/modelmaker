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

import org.jspecify.annotations.NullMarked;

/**
 * Schema parsing and source emitters shared by the {@code ModelMaker} build-tool plugins - free of any
 * build-tool API.
 */
@NullMarked
module io.github.malczuuu.modelmaker {
  requires com.google.gson;
  requires static org.jspecify;

  exports io.github.malczuuu.modelmaker;
}
