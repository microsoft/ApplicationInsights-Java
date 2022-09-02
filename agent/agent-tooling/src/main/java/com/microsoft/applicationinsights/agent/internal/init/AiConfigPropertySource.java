// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.Collections;
import java.util.Map;

@AutoService(ConfigPropertySource.class)
public class AiConfigPropertySource implements ConfigPropertySource {
  @Override
  public Map<String, String> getProperties() {
    // TODO (trask) this is temporary until 1.18.0, see
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6491
    return Collections.singletonMap("otel.instrumentation.common.default-enabled", "false");
  }
}
