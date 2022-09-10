// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.CODE_NAMESPACE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.ENDUSER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ApplicationInsightsWebTest {

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void setRequestProperty() {
    // when
    Code.setProperty();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setProperty")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"),
                            equalTo(CODE_FUNCTION, "setProperty"),
                            equalTo(stringKey("akey"), "avalue")),
                span ->
                    span.hasName("Code.internalSetProperty")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void setRequestUser() {
    // when
    Code.setUser();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setUser")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"),
                            equalTo(CODE_FUNCTION, "setUser"),
                            equalTo(ENDUSER_ID, "myuser")),
                span ->
                    span.hasName("Code.internalSetUser")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void setRequestName() {
    // when
    Code.setName();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("new name")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"), equalTo(CODE_FUNCTION, "setName")),
                span ->
                    span.hasName("Code.internalSetName")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void setSuccess() {
    // when
    Code.setSuccess();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setSuccess")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"), equalTo(CODE_FUNCTION, "setSuccess")),
                span ->
                    span.hasName("Code.internalSetSuccess")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void setSource() {
    // when
    Code.setSource();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setSource")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"),
                            equalTo(CODE_FUNCTION, "setSource"),
                            equalTo(
                                stringKey("applicationinsights.internal.source"), "the source")),
                span ->
                    span.hasName("Code.internalSetSource")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void getRequestId() {
    // when
    String spanId = Code.getId();

    // then
    testing.waitAndAssertTraces(
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName("Code.getId")
                      .hasKind(SERVER)
                      .hasNoParent()
                      .hasAttributesSatisfyingExactly(
                          equalTo(CODE_NAMESPACE, "Code"), equalTo(CODE_FUNCTION, "getId")),
              span ->
                  span.hasName("Code.internalGetId").hasKind(INTERNAL).hasParent(trace.getSpan(0)));
          assertThat(trace.getSpan(0).getSpanId()).isEqualTo(spanId);
        });
  }

  @Test
  void getOperationId() {
    // when
    String traceId = Code.getOperationId();

    // then
    testing.waitAndAssertTraces(
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName("Code.getOperationId")
                      .hasKind(SERVER)
                      .hasNoParent()
                      .hasAttributesSatisfyingExactly(
                          equalTo(CODE_NAMESPACE, "Code"),
                          equalTo(CODE_FUNCTION, "getOperationId")),
              span ->
                  span.hasName("Code.internalGetOperationId")
                      .hasKind(INTERNAL)
                      .hasParent(trace.getSpan(0)));
          assertThat(trace.getSpan(0).getTraceId()).isEqualTo(traceId);
        });
  }

  @Test
  void setSessionId() {
    // when
    Code.setSessionId();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setSessionId")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"),
                            equalTo(CODE_FUNCTION, "setSessionId"),
                            equalTo(
                                stringKey("applicationinsights.internal.session_id"),
                                "the session id")),
                span ->
                    span.hasName("Code.internalSetSessionId")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void setOperatingSystem() {
    // when
    Code.setOperatingSystem();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setOperatingSystem")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"),
                            equalTo(CODE_FUNCTION, "setOperatingSystem"),
                            equalTo(
                                stringKey("applicationinsights.internal.operating_system"),
                                "the operating system")),
                span ->
                    span.hasName("Code.internalSetOperatingSystem")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void setOperatingSystemVersion() {
    // when
    Code.setOperatingSystemVersion();

    // then
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("Code.setOperatingSystemVersion")
                        .hasKind(SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(CODE_NAMESPACE, "Code"),
                            equalTo(CODE_FUNCTION, "setOperatingSystemVersion"),
                            equalTo(
                                stringKey("applicationinsights.internal.operating_system_version"),
                                "the operating system version")),
                span ->
                    span.hasName("Code.internalSetOperatingSystemVersion")
                        .hasKind(INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void getTracestate() {
    SpanContext spanContext =
        SpanContext.create(
            "12341234123412341234123412341234",
            "1234123412341234",
            TraceFlags.getDefault(),
            TraceState.builder().put("one", "1").put("two", "2").build());

    // when
    Tracestate tracestate;
    try (Scope ignored = Context.root().with(Span.wrap(spanContext)).makeCurrent()) {
      tracestate = Code.getTracestate();
    }

    // then
    assertThat(tracestate.get("one")).isEqualTo("1");
    assertThat(tracestate.get("two")).isEqualTo("2");
  }

  @Test
  void getTraceflag() {
    checkTraceFlags(TraceFlags.getDefault());
    checkTraceFlags(TraceFlags.getSampled());
  }

  private static void checkTraceFlags(TraceFlags flags) {
    SpanContext spanContext =
        SpanContext.create(
            "12341234123412341234123412341234", "1234123412341234", flags, TraceState.getDefault());

    // when
    int traceflag;
    try (Scope ignored = Context.root().with(Span.wrap(spanContext)).makeCurrent()) {
      traceflag = Code.getTraceflag();
    }

    // then
    assertThat(traceflag).isEqualTo(flags.asByte());
  }

  @Test
  void shouldInteropWithGenerateChildDependencyTraceparent() {
    SpanContext spanContext =
        SpanContext.create(
            "12341234123412341234123412341234",
            "1234123412341234",
            TraceFlags.getDefault(),
            TraceState.getDefault());
    Context parent = Context.root().with(Span.wrap(spanContext));
    Span span =
        GlobalOpenTelemetry.getTracer("test").spanBuilder("test").setParent(parent).startSpan();

    // when
    Object traceparent;
    try (Scope ignored = parent.with(span).makeCurrent()) {
      traceparent = TraceContextCorrelation.generateChildDependencyTraceparent();
    }

    // then
    assertThat(traceparent).isNotNull();
  }

  @Test
  void shouldInteropWithRetriveTracestate() {
    checkTraceState(TraceState.getDefault(), null);
    checkTraceState(TraceState.builder().put("one", "1").build(), "one=1");
  }

  private static void checkTraceState(
      TraceState otelTraceState, @Nullable String legacyTracestate) {
    SpanContext spanContext =
        SpanContext.create(
            "12341234123412341234123412341234",
            "1234123412341234",
            TraceFlags.getDefault(),
            otelTraceState);

    // when
    String traceparent;
    try (Scope ignored = Context.root().with(Span.wrap(spanContext)).makeCurrent()) {
      traceparent = Code.retriveTracestate();
    }

    // then
    assertThat(traceparent).isEqualTo(legacyTracestate);
  }

  @Test
  void shouldNotThrowOnOtherRequestTelemetryContextMethods() {
    Code.otherRequestTelemetryContextMethods();
  }

  @Test
  void shouldNotThrowOnOtherRequestTelemetryMethods() {
    Code.otherRequestTelemetryMethods();
  }

  @Test
  void shouldNotThrowOnOtherBaseTelemetryMethods() {
    Code.otherBaseTelemetryMethods();
  }

  @Test
  void shouldNotThrowOnOtherTelemetryContextMethods() {
    Code.otherTelemetryContextMethods();
  }

  @Test
  void shouldNotThrowOnOtherUserContextMethods() {
    Code.otherUserContextMethods();
  }

  @Test
  void shouldNotThrowOnOtherOperationContextMethods() {
    Code.otherOperationContextMethods();
  }

  @Test
  void shouldNotThrowOnOtherSessionContextMethods() {
    Code.otherSessionContextMethods();
  }

  @Test
  void shouldNotThrowOnOtherDeviceContextMethods() {
    Code.otherDeviceContextMethods();
  }
}
