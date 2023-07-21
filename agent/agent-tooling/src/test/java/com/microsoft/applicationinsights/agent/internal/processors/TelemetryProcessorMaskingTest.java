// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.SemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TelemetryProcessorMaskingTest {

  @Test
  void shouldMaskAttribute() {

    String httpAttributeKey = "http.url";
    String httpUrl = "https://user/123456789";

    String userGroupName = "userGroupName";

    String userGroupNameInPattern = "?<" + userGroupName + ">";
    String userGroup = "(" + userGroupNameInPattern + "[a-zA-Z.:\\/]+" + ")";
    String numbers = "\\d+";
    String regEx = "^" + userGroup + numbers;

    String mask = "**";
    String replacementPattern = "${" + userGroupName + "}" + mask;

    SpanData newSpanData =
        maskingAttributeProcessor(httpAttributeKey, regEx, replacementPattern)
            .processActions(new RequestSpanData(httpUrl));

    Attributes newAttributes = newSpanData.getAttributes();
    String newHttpUrlAttributeValue = newAttributes.get(SemanticAttributes.HTTP_URL);
    assertThat(newHttpUrlAttributeValue).isEqualTo("https://user/" + mask);
  }

  @NotNull
  private static AttributeProcessor maskingAttributeProcessor(
      String httpAttributeKey, String regEx, String replacementPattern) {
    Configuration.ProcessorAction maskingAction =
        new Configuration.ProcessorAction(
            httpAttributeKey,
            Configuration.ProcessorActionType.MASK,
            null,
            null,
            regEx,
            replacementPattern);

    Configuration.ProcessorConfig processorConfig = new Configuration.ProcessorConfig();
    processorConfig.type = Configuration.ProcessorType.ATTRIBUTE;
    processorConfig.include = null;
    processorConfig.exclude = null;
    processorConfig.actions = Collections.singletonList(maskingAction);

    return AttributeProcessor.create(processorConfig, false);
  }

  static class RequestSpanData implements SpanData {

    private static final String TRACE_ID = TraceId.fromLongs(10L, 2L);
    private static final String SPAN_ID = SpanId.fromLong(1);
    private static final TraceState TRACE_STATE = TraceState.builder().build();
    private final String httpUrl;

    public RequestSpanData(String httpUrlAttributeValue) {
      this.httpUrl = httpUrlAttributeValue;
    }

    @Override
    public SpanContext getSpanContext() {
      return SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TRACE_STATE);
    }

    @Override
    public String getTraceId() {
      return TRACE_ID;
    }

    @Override
    public String getSpanId() {
      return SPAN_ID;
    }

    @Override
    public SpanContext getParentSpanContext() {
      return SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TRACE_STATE);
    }

    @Override
    public String getParentSpanId() {
      return SpanId.fromLong(1);
    }

    @Override
    public Resource getResource() {
      return Resource.create(Attributes.empty());
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
      return InstrumentationLibraryInfo.create("TestLib", "1");
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
      return InstrumentationScopeInfo.create("TestLib", "1", null);
    }

    @Override
    public String getName() {
      return "/service/resource";
    }

    @Override
    public SpanKind getKind() {
      return SpanKind.INTERNAL;
    }

    @Override
    public long getStartEpochNanos() {
      return MILLISECONDS.toNanos(Instant.now().toEpochMilli());
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.builder()
          .put("http.status_code", 200L)
          .put("http.url", httpUrl)
          .put("http.method", "GET")
          .put("ai.sampling.percentage", 100.0)
          .build();
    }

    @Override
    public List<EventData> getEvents() {
      return new ArrayList<>();
    }

    @Override
    public List<LinkData> getLinks() {
      return new ArrayList<>();
    }

    @Override
    public StatusData getStatus() {
      return StatusData.ok();
    }

    @Override
    public long getEndEpochNanos() {
      return MILLISECONDS.toNanos(Instant.now().toEpochMilli());
    }

    @Override
    public boolean hasEnded() {
      return false;
    }

    @Override
    public int getTotalRecordedEvents() {
      return 0;
    }

    @Override
    public int getTotalRecordedLinks() {
      return 0;
    }

    @Override
    public int getTotalAttributeCount() {
      return 0;
    }
  }
}
