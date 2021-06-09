/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class ApplicationInsightsWebInstrumentationModule extends InstrumentationModule {
  public ApplicationInsightsWebInstrumentationModule() {
    super("applicationinsights-web", "applicationinsights-web-2.1");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new ThreadContextInstrumentation(),
        new RequestTelemetryContextInstrumentation(),
        new BaseTelemetryInstrumentation(),
        new RequestTelemetryInstrumentation(),
        new TelemetryContextInstrumentation(),
        new UserContextInstrumentation(),
        new OperationContextInstrumentation());
  }
}
