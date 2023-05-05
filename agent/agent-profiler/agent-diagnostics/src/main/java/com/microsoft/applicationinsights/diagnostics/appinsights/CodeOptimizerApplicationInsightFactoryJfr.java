// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.appinsights;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngineFactory;
import java.util.concurrent.ScheduledExecutorService;

/** Factory for Code Optimizer diagnostics to be service loaded */
@AutoService(DiagnosticEngineFactory.class)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CodeOptimizerApplicationInsightFactoryJfr implements DiagnosticEngineFactory {
  @Override
  public DiagnosticEngine create(ScheduledExecutorService executorService) {
    return new CodeOptimizerDiagnosticEngineJfr(executorService);
  }
}
