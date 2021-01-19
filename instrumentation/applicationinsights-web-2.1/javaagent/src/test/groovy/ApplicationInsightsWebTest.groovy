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
    def spanId = new Code().getOperationId()

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

    TEST_WRITER.getTraces().get(0).get(0).spanId == spanId
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
