// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class ApplicationInsightsWebInstrumentationModule extends InstrumentationModule {
  public ApplicationInsightsWebInstrumentationModule() {
    super("ai-applicationinsights-web");
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
        new OperationContextInstrumentation(),
        new SessionContextInstrumentation(),
        new DeviceContextInstrumentation());
  }
}
