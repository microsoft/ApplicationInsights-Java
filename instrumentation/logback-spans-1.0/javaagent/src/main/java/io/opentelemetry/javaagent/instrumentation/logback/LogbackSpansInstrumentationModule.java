/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.logback;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class LogbackSpansInstrumentationModule extends InstrumentationModule {
  public LogbackSpansInstrumentationModule() {
    // this name is important currently because it's used to disable this instrumentation
    super("logback-spans");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new LogbackSpansInstrumentation());
  }
}
