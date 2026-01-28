// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.methods.ai;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.incubator.config.internal.DeclarativeConfigUtil;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.tooling.config.MethodsConfigurationParser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(InstrumentationModule.class)
public class MethodInstrumentationModule extends InstrumentationModule {

  private static final String TRACE_METHODS_CONFIG = "applicationinsights.internal.methods.include";

  private final List<TypeInstrumentation> typeInstrumentations;

  public MethodInstrumentationModule() {
    super("ai-methods");

    // First try to get config from DeclarativeConfigUtil
    DeclarativeConfigProperties config =
        DeclarativeConfigUtil.getInstrumentationConfig(GlobalOpenTelemetry.get(), "ai-methods");
    String include = config.getString(TRACE_METHODS_CONFIG);
    
    // Fallback to system property if not found in declarative config
    if (include == null) {
      include = System.getProperty(TRACE_METHODS_CONFIG);
    }

    Map<String, Set<String>> classMethodsToTrace =
        include != null ? MethodsConfigurationParser.parse(include) : emptyMap();

    typeInstrumentations =
        classMethodsToTrace.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(e -> new MethodInstrumentation(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
  }

  // the default configuration has empty "otel.instrumentation.methods.include", and so doesn't
  // generate any TypeInstrumentation for muzzle to analyze
  @Override
  public List<String> getAdditionalHelperClassNames() {
    return typeInstrumentations.isEmpty()
        ? emptyList()
        : asList(
            "io.opentelemetry.javaagent.instrumentation.methods.ai.MethodSingletons",
            "io.opentelemetry.javaagent.instrumentation.methods.ai.MethodSingletons$MethodSpanKindExtractor");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return typeInstrumentations;
  }
}
