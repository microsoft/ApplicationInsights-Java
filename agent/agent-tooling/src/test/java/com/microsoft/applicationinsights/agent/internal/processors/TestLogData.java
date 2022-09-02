// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.resources.Resource;

public class TestLogData implements LogData {

  private final Resource resource;
  private final InstrumentationScopeInfo instrumentationScopeInfo;
  private final long epochNanos;
  private final SpanContext spanContext;
  private final Severity severity;
  private final String severityText;
  private final Body body;
  private final Attributes attributes;

  static TestLogData.Builder builder() {
    return new Builder();
  }

  TestLogData(Builder builder) {
    this.resource = builder.resource;
    this.instrumentationScopeInfo = builder.instrumentationScopeInfo;
    this.epochNanos = builder.epochNanos;
    this.spanContext = builder.spanContext;
    this.severity = builder.severity;
    this.severityText = builder.severityText;
    this.body = builder.body;
    this.attributes = builder.attributes;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return instrumentationScopeInfo;
  }

  @Override
  public long getEpochNanos() {
    return epochNanos;
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext;
  }

  @Override
  public Severity getSeverity() {
    return severity;
  }

  @Override
  public String getSeverityText() {
    return severityText;
  }

  @Override
  public Body getBody() {
    return body;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  static class Builder {
    private Resource resource;
    private InstrumentationScopeInfo instrumentationScopeInfo;
    private long epochNanos;
    private SpanContext spanContext;
    private Severity severity;
    private String severityText;
    private Body body;
    private Attributes attributes;

    public Builder setResource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder setInstrumentationScopeInfo(InstrumentationScopeInfo instrumentationScopeInfo) {
      this.instrumentationScopeInfo = instrumentationScopeInfo;
      return this;
    }

    public Builder setEpochNanos(long epochNanos) {
      this.epochNanos = epochNanos;
      return this;
    }

    public Builder setSpanContext(SpanContext spanContext) {
      this.spanContext = spanContext;
      return this;
    }

    public Builder setSeverity(Severity severity) {
      this.severity = severity;
      return this;
    }

    public Builder setSeverityText(String text) {
      this.severityText = text;
      return this;
    }

    public Builder setBody(Body body) {
      this.body = body;
      return this;
    }

    public Builder setAttributes(Attributes attributes) {
      this.attributes = attributes;
      return this;
    }

    public TestLogData build() {
      return new TestLogData(this);
    }
  }
}
