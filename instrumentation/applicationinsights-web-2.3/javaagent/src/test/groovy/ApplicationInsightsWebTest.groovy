/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification

import static io.opentelemetry.api.trace.SpanKind.INTERNAL
import static io.opentelemetry.api.trace.SpanKind.SERVER

class ApplicationInsightsWebTest extends AgentInstrumentationSpecification {

  def "set request property"() {
    when:
    Code.setProperty()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setProperty"
          kind SERVER
          hasNoParent()
          attributes {
            "akey" "avalue"
          }
        }
        span(1) {
          name "Code.internalSetProperty"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "set request user"() {
    when:
    Code.setUser()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setUser"
          kind SERVER
          hasNoParent()
          attributes {
            "enduser.id" "myuser"
          }
        }
        span(1) {
          name "Code.internalSetUser"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "set request name"() {
    when:
    Code.setName()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "new name"
          kind SERVER
          hasNoParent()
        }
        span(1) {
          name "Code.internalSetName"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "set success"() {
    when:
    Code.setSuccess()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setSuccess"
          kind SERVER
          hasNoParent()
          status StatusCode.ERROR
        }
        span(1) {
          name "Code.internalSetSuccess"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "set source"() {
    when:
    Code.setSource()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setSource"
          kind SERVER
          hasNoParent()
          attributes {
            "applicationinsights.internal.source" "the source"
          }
        }
        span(1) {
          name "Code.internalSetSource"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "get request id"() {
    when:
    def spanId = Code.getId()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.getId"
          kind SERVER
          hasNoParent()
        }
        span(1) {
          name "Code.internalGetId"
          kind INTERNAL
          childOf span(0)
        }
      }
    }

    getTraces().get(0).get(0).spanId == spanId
  }

  def "get operation id"() {
    when:
    def traceId = Code.getOperationId()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.getOperationId"
          kind SERVER
          hasNoParent()
        }
        span(1) {
          name "Code.internalGetOperationId"
          kind INTERNAL
          childOf span(0)
        }
      }
    }

    getTraces().get(0).get(0).traceId == traceId
  }

  def "set session id"() {
    when:
    Code.setSessionId()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setSessionId"
          kind SERVER
          hasNoParent()
          attributes {
            "applicationinsights.internal.session_id" "the session id"
          }
        }
        span(1) {
          name "Code.internalSetSessionId"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "set operating system"() {
    when:
    Code.setOperatingSystem()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setOperatingSystem"
          kind SERVER
          hasNoParent()
          attributes {
            "applicationinsights.internal.operating_system" "the operating system"
          }
        }
        span(1) {
          name "Code.internalSetOperatingSystem"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "set operating system version"() {
    when:
    Code.setOperatingSystemVersion()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "Code.setOperatingSystemVersion"
          kind SERVER
          hasNoParent()
          attributes {
            "applicationinsights.internal.operating_system_version" "the operating system version"
          }
        }
        span(1) {
          name "Code.internalSetOperatingSystemVersion"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "get tracestate"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      TraceFlags.getDefault(),
      TraceState.builder().put("one", "1").put("two", "2").build())

    when:
    def scope = Context.root().with(Span.wrap(spanContext)).makeCurrent()
    def tracestate = Code.getTracestate()
    scope.close()

    then:
    tracestate.get("one") == "1"
    tracestate.get("two") == "2"
  }

  def "get traceflag"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      flag,
      TraceState.getDefault())

    when:
    def scope = Context.root().with(Span.wrap(spanContext)).makeCurrent()
    def traceflag = Code.getTraceflag()
    scope.close()

    then:
    traceflag == flag.asByte()

    where:
    flag << [TraceFlags.getDefault(), TraceFlags.getSampled()]
  }

  def "should interop with generateChildDependencyTraceparent"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      TraceFlags.getDefault(),
      TraceState.getDefault())
    def parent = Context.root().with(Span.wrap(spanContext))
    def span = GlobalOpenTelemetry.getTracer("test")
      .spanBuilder("test")
      .setParent(parent)
      .startSpan()

    when:
    Scope scope = parent.with(span).makeCurrent()
    def traceparent
    try {
      traceparent = TraceContextCorrelation.generateChildDependencyTraceparent()
    } finally {
      scope.close()
    }

    then:
    traceparent != null
  }

  def "should interop with retriveTracestate"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      TraceFlags.getDefault(),
      otelTraceState)

    when:
    def scope = Context.root().with(Span.wrap(spanContext)).makeCurrent()
    def traceparent = Code.retriveTracestate()
    scope.close()

    then:
    traceparent == legacyTracestate

    where:
    otelTraceState                               | legacyTracestate
    TraceState.getDefault()                      | "az=1234"
    TraceState.builder().put("one", "1").build() | "az=1234,one=1"
  }

  def "should not throw on other RequestTelemetryContext methods"() {
    expect:
    Code.otherRequestTelemetryContextMethods()
  }

  def "should not throw on other RequestTelemetry methods"() {
    expect:
    Code.otherRequestTelemetryMethods()
  }

  def "should not throw on other BaseTelemetry methods"() {
    expect:
    Code.otherBaseTelemetryMethods()
  }

  def "should not throw on other TelemetryContext methods"() {
    expect:
    Code.otherTelemetryContextMethods()
  }

  def "should not throw on other UserContext methods"() {
    expect:
    Code.otherUserContextMethods()
  }

  def "should not throw on other OperationContext methods"() {
    expect:
    Code.otherOperationContextMethods()
  }

  def "should not throw on other SessionContext methods"() {
    expect:
    Code.otherSessionContextMethods()
  }

  def "should not throw on other DeviceContext methods"() {
    expect:
    Code.otherDeviceContextMethods()
  }
}
