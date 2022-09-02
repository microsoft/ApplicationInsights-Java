// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import java.util.concurrent.Future;

/** Engine that will be invoked on an AlertBreach. */
public interface DiagnosticEngine {

  /** Invoked on application startup. */
  void init();

  /**
   * Perform a diagnostic cycle. It is expected that this will execute and return within the time in
   * alertBreach.alertConfiguration.profileDuration
   */
  Future<DiagnosisResult<?>> performDiagnosis(AlertBreach alertBreach);
}
