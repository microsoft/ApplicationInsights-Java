// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Factory to be invoked to create a DiagnosticEngine. This factory will be service loaded by the
 * agent and invoked. It is up to the provider of a DiagnosticEngine to provide a service loader for
 * this interface.
 */
public interface DiagnosticEngineFactory {
  DiagnosticEngine create(ScheduledExecutorService executorService);
}
