/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.api.trace.Span.Kind.INTERNAL
import static io.opentelemetry.api.trace.Span.Kind.SERVER

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

  def "update request name"() {
    when:
    new Code().updateName()

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "new name"
          kind SERVER
          hasNoParent()
        }
        span(1) {
          name "Code.internalUpdateName"
          kind INTERNAL
          childOf span(0)
        }
      }
    }
  }

  def "should throw on other RequestTelemetryContext methods"() {
    when:
    new Code().otherRequestTelemetryContextMethods()

    then:
    def exception = thrown(Exception)
    exception.getMessage().contains("ThreadContext.getRequestTelemetryContext().getCorrelationContext() is not supported")
  }

  def "should throw on other RequestTelemetry methods"() {
    when:
    new Code().otherRequestTelemetryMethods()

    then:
    def exception = thrown(Exception)
    exception.getMessage().contains("ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getName() is not supported")
  }

  def "should throw on other BaseTelemetry methods"() {
    when:
    new Code().otherBaseTelemetryMethods()

    then:
    def exception = thrown(Exception)
    exception.getMessage().contains("ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getTimestamp() is not supported")
  }

  def "should throw on other TelemetryContext methods"() {
    when:
    new Code().otherTelemetryContextMethods()

    then:
    def exception = thrown(Exception)
    exception.getMessage().contains("ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext().getSession() is not supported")
  }

  def "should throw on other UserContext methods"() {
    when:
    new Code().otherUserContextMethods()

    then:
    def exception = thrown(Exception)
    exception.getMessage().contains("ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext().getUser().setAccountId() is not supported")
  }
}
