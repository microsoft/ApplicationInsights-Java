// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import java.util.concurrent.Future;

/** Engine that will be invoked on an AlertBreach. */
public interface DiagnosticEngine {

  /** Invoked on application startup. */
  void init(int thisPid);

  /**
   * Perform a diagnostic cycle. It is expected that this will execute and return within the time in
   * alertBreach.alertConfiguration.profileDuration
   */
  Future<DiagnosisResult<?>> performDiagnosis(AlertBreach alertBreach);

  /**
   * Start collecting diagnostics continuously.
   *
   * <p>Used with continuous profiling, where a circular buffer is dumped on demand rather than a
   * forward-looking recording being created per breach. Registering the periodic diagnostic
   * emitters up front ensures diagnostic events populate the continuous recording buffer, so they
   * are present in any snapshot that is dumped.
   *
   * @return {@code true} if continuous diagnostics were started successfully; {@code false} if this
   *     engine does not support continuous diagnostics or startup failed. The default
   *     implementation returns {@code false} so alternate implementations do not silently appear to
   *     support continuous diagnostics while doing nothing.
   */
  default boolean startContinuousDiagnostics() {
    return false;
  }

  /** Stop collecting diagnostics continuously. Defaults to a no-op. */
  default void stopContinuousDiagnostics() {}
}
