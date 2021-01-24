/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.api.trace.Span.Kind.INTERNAL
import static io.opentelemetry.api.trace.Span.Kind.SERVER

import com.microsoft.applicationinsights.web.internal.ThreadContext
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.test.AgentTestRunner

class ApplicationInsightsWebTest extends AgentTestRunner {

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

    TEST_WRITER.getTraces().get(0).get(0).spanId == spanId
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

    TEST_WRITER.getTraces().get(0).get(0).traceId == traceId
  }

  def "get tracestate"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      TraceFlags.getDefault(),
      TraceState.builder().set("one", "1").set("two", "2").build())
    def parent = Context.root().with(Span.wrap(spanContext))
    def span = OpenTelemetry.getGlobalTracer("test")
      .spanBuilder("test")
      .setParent(parent)
      .startSpan()

    when:
    Scope scope = parent.with(span).makeCurrent()
    def tracestate = null
    try {
      tracestate = ThreadContext.getRequestTelemetryContext().getTracestate()
    } finally {
      scope.close()
    }

    then:
    tracestate.get("one") == "1"
    tracestate.get("two") == "2"
  }

  def "get traceflag"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      (byte) flag,
      TraceState.getDefault())
    def parent = Context.root().with(Span.wrap(spanContext))
    def span = OpenTelemetry.getGlobalTracer("test")
      .spanBuilder("test")
      .setParent(parent)
      .startSpan()

    when:
    Scope scope = parent.with(span).makeCurrent()
    def traceflag = 0
    try {
      traceflag = ThreadContext.getRequestTelemetryContext().getTraceflag()
    } finally {
      scope.close()
    }

    then:
    traceflag == flag

    where:
    flag << [0, 1]
  }

  def "should interop with generateChildDependencyTraceparent"() {
    def spanContext = SpanContext.create(
      "12341234123412341234123412341234",
      "1234123412341234",
      TraceFlags.getDefault(),
      TraceState.getDefault())
    def parent = Context.root().with(Span.wrap(spanContext))
    def span = OpenTelemetry.getGlobalTracer("test")
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
