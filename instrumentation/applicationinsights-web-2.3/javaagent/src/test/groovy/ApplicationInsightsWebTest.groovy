/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.api.trace.SpanKind.INTERNAL
import static io.opentelemetry.api.trace.SpanKind.SERVER

import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification

class ApplicationInsightsWebTest extends AgentInstrumentationSpecification {

  def "set request property"() {
    when:
    new Code().setProperty()

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
    new Code().setUser()

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
    new Code().setName()

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

  def "set source"() {
    when:
    new Code().setSource()

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
    def spanId = new Code().getId()

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
    def traceId = new Code().getOperationId()

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

  def "get tracestate"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      TraceFlags.getDefault(),
      TraceState.builder().put("one", "1").put("two", "2").build())

    when:
    def scope = Context.root().with(Span.wrap(spanContext)).makeCurrent()
    def tracestate = new Code().getTracestate()
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
    def traceflag = new Code().getTraceflag()
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
    def traceparent = new Code().retriveTracestate()
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
    new Code().otherRequestTelemetryContextMethods()
  }

  def "should not throw on other RequestTelemetry methods"() {
    expect:
    new Code().otherRequestTelemetryMethods()
  }

  def "should not throw on other BaseTelemetry methods"() {
    expect:
    new Code().otherBaseTelemetryMethods()
  }

  def "should not throw on other TelemetryContext methods"() {
    expect:
    new Code().otherTelemetryContextMethods()
  }

  def "should not throw on other UserContext methods"() {
    expect:
    new Code().otherUserContextMethods()
  }

  def "should not throw on other OperationContext methods"() {
    expect:
    new Code().otherOperationContextMethods()
  }
}
