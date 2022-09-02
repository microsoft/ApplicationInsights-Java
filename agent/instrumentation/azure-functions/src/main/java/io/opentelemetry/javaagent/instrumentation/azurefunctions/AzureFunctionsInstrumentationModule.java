// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class AzureFunctionsInstrumentationModule extends InstrumentationModule {

  public AzureFunctionsInstrumentationModule() {
    super("ai-azure-functions");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new InvocationInstrumentation(), new FunctionEnvironmentReloadInstrumentation());
  }
}
