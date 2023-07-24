/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.streaming.ai.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class HuggingFaceConfig {
  // for API compute provider
  @JsonProperty(
    value = "api-url",
    defaultValue = "https://api-inference.huggingface.co/pipeline/feature-extraction/"
  )
  private String apiUrl = "https://api-inference.huggingface.co/pipeline/feature-extraction/";

  @JsonProperty(value = "model-check-url", defaultValue = "https://huggingface.co/api/models/")
  private String modelUrl = "https://huggingface.co/api/models/";

  // for API compute provider
  @JsonProperty(value = "access-key")
  private String accessKey;

  @JsonProperty ComputeProvider provider;
}
