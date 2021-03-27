/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.azure.core.util.tracing.TracerProxy
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification

class AzureSdkTest extends AgentInstrumentationSpecification {

  def "test helper classes injected"() {
    expect:
    TracerProxy.isTracingEnabled()
  }
}
