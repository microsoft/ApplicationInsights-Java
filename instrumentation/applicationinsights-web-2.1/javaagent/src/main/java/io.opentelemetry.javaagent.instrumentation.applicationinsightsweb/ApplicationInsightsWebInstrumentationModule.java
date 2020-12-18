/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoService(InstrumentationModule.class)
public class ApplicationInsightsWebInstrumentationModule extends InstrumentationModule {
  public ApplicationInsightsWebInstrumentationModule() {
    super("applicationinsights-web", "applicationinsights-web-2.1");
  }

  @Override
  public Map<String, String> contextStore() {
    Map<String, String> mappings = new HashMap<>();
    mappings.put(
        "com.microsoft.applicationinsights.web.internal.RequestTelemetryContext",
        Span.class.getName());
    mappings.put(
        "com.microsoft.applicationinsights.telemetry.RequestTelemetry", Span.class.getName());
    mappings.put(
        "com.microsoft.applicationinsights.telemetry.TelemetryContext", Span.class.getName());
    mappings.put(
        "com.microsoft.applicationinsights.extensibility.context.UserContext",
        Span.class.getName());
    return mappings;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new ThreadContextInstrumentation(),
        new RequestTelemetryContextInstrumentation(),
        new BaseTelemetryInstrumentation(),
        new RequestTelemetryInstrumentation(),
        new TelemetryContextInstrumentation(),
        new UserContextInstrumentation());
  }
}
