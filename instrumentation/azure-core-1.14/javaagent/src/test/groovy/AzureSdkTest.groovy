/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.azure.core.util.Context
import com.azure.core.util.tracing.TracerProxy
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification

class AzureSdkTest extends AgentInstrumentationSpecification {

  def "test helper classes injected"() {
    expect:
    TracerProxy.isTracingEnabled()
  }

  def "test span"() {
    when:
    Context context = TracerProxy.start("hello", Context.NONE)
    TracerProxy.end(200, null, context)

    then:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "hello"
          status StatusCode.OK
          attributes {
          }
        }
      }
    }
  }
}
