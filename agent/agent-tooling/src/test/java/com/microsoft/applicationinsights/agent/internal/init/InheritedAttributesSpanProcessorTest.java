// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.sdk.testing.assertj.TracesAssert.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class InheritedAttributesSpanProcessorTest {

  private final InMemorySpanExporter exporter = InMemorySpanExporter.create();

  private final AttributeKey<String> oneStringKey = AttributeKey.stringKey("one");
  private final AttributeKey<Long> oneLongKey = AttributeKey.longKey("one");

  @AfterEach
  public void afterEach() {
    exporter.reset();
  }

  @Test
  public void shouldNotInheritAttribute() {
    Tracer tracer = newTracer(Collections.emptyList());
    Span span =
        tracer.spanBuilder("parent").setNoParent().setAttribute(oneStringKey, "1").startSpan();
    Context context = Context.root().with(span);
    try {
      tracer.spanBuilder("child").setParent(context).startSpan().end();
    } finally {
      span.end();
    }

    await().until(() -> exporter.getFinishedSpanItems().size() == 2);

    assertThat(Collections.singleton(exporter.getFinishedSpanItems()))
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    childSpan ->
                        childSpan.hasName("child").hasKind(INTERNAL).hasTotalAttributeCount(0),
                    parentSpan ->
                        parentSpan
                            .hasName("parent")
                            .hasKind(INTERNAL)
                            .hasAttributesSatisfying(
                                attributes ->
                                    OpenTelemetryAssertions.assertThat(attributes)
                                        .containsOnly(entry(oneStringKey, "1")))));
  }

  @Test
  public void shouldInheritAttribute() {
    Configuration.InheritedAttribute inheritedAttribute = new Configuration.InheritedAttribute();
    inheritedAttribute.key = "one";
    inheritedAttribute.type = Configuration.AttributeType.STRING;

    Tracer tracer = newTracer(Collections.singletonList(inheritedAttribute));
    Span span =
        tracer.spanBuilder("parent").setNoParent().setAttribute(oneStringKey, "1").startSpan();
    Context context = Context.root().with(span);
    try {
      tracer.spanBuilder("child").setParent(context).startSpan().end();
    } finally {
      span.end();
    }

    await().until(() -> exporter.getFinishedSpanItems().size() == 2);

    assertThat(Collections.singleton(exporter.getFinishedSpanItems()))
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    childSpan ->
                        childSpan
                            .hasName("child")
                            .hasKind(INTERNAL)
                            .hasAttributesSatisfying(
                                attributes ->
                                    OpenTelemetryAssertions.assertThat(attributes)
                                        .containsOnly(entry(oneStringKey, "1"))),
                    parentSpan ->
                        parentSpan
                            .hasName("parent")
                            .hasKind(INTERNAL)
                            .hasAttributesSatisfying(
                                attributes ->
                                    OpenTelemetryAssertions.assertThat(attributes)
                                        .containsOnly(entry(oneStringKey, "1")))));
  }

  @Test
  public void shouldNotInheritAttributeWithSameNameButDifferentType() {
    Configuration.InheritedAttribute inheritedAttribute = new Configuration.InheritedAttribute();
    inheritedAttribute.key = "one";
    inheritedAttribute.type = Configuration.AttributeType.STRING;

    Tracer tracer = newTracer(Collections.singletonList(inheritedAttribute));
    Span span = tracer.spanBuilder("parent").setNoParent().setAttribute(oneLongKey, 1L).startSpan();
    Context context = Context.root().with(span);
    try {
      tracer.spanBuilder("child").setParent(context).startSpan().end();
    } finally {
      span.end();
    }

    await().until(() -> exporter.getFinishedSpanItems().size() == 2);

    assertThat(Collections.singleton(exporter.getFinishedSpanItems()))
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    childSpan ->
                        childSpan.hasName("child").hasKind(INTERNAL).hasTotalAttributeCount(0),
                    parentSpan ->
                        parentSpan
                            .hasName("parent")
                            .hasKind(INTERNAL)
                            .hasAttributesSatisfying(
                                attributes ->
                                    OpenTelemetryAssertions.assertThat(attributes)
                                        .containsOnly(entry(oneLongKey, 1L)))));
  }

  private Tracer newTracer(List<Configuration.InheritedAttribute> inheritedAttributes) {
    OpenTelemetrySdk sdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(new InheritedAttributesSpanProcessor(inheritedAttributes))
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    .build())
            .build();
    return sdk.getTracer("test");
  }
}
