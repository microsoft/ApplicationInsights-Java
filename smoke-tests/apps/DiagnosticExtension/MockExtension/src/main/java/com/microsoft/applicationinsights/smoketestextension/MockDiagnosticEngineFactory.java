// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestextension;

import com.microsoft.applicationinsights.diagnostics.DiagnosisResult;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngineFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class MockDiagnosticEngineFactory implements DiagnosticEngineFactory {

  @Override
  public DiagnosticEngine create(ScheduledExecutorService executorService) {
    return new DiagnosticEngine() {

      @Override
      public void init() {
        System.setProperty("DIAGNOSTIC_CALLED", "true");
      }

      @Override
      public Future<DiagnosisResult<?>> performDiagnosis(
          com.microsoft.applicationinsights.alerting.alert.AlertBreach alertBreach) {
        return CompletableFuture.completedFuture(null);
      }
    };
  }
}
