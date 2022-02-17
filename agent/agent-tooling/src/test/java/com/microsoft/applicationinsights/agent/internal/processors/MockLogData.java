/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

public class MockLogData implements LogData {

  private final Resource resource;
  private final InstrumentationLibraryInfo instrumentationLibraryInfo;
  private final long epochNanos;
  private final SpanContext spanContext;
  private final Severity severity;
  private final String severityText;
  private final String name;
  private final Body body;
  private final Attributes attributes;

  static MockLogData.Builder builder() {
    return new Builder();
  }

  MockLogData(Builder builder) {
    this.resource = builder.resource;
    this.instrumentationLibraryInfo = builder.instrumentationLibraryInfo;
    this.epochNanos = builder.epochNanos;
    this.spanContext = builder.spanContext;
    this.severity = builder.severity;
    this.severityText = builder.severityText;
    this.name = builder.name;
    this.body = builder.body;
    this.attributes = builder.attributes;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return instrumentationLibraryInfo;
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

  @Nullable
  @Override
  public String getSeverityText() {
    return severityText;
  }

  @Nullable
  @Override
  public String getName() {
    return name;
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
    private InstrumentationLibraryInfo instrumentationLibraryInfo;
    private long epochNanos;
    private SpanContext spanContext;
    private Severity severity;
    private String severityText;
    private String name;
    private Body body;
    private Attributes attributes;

    public Builder setResource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder setInstrumentationLibraryInfo(InstrumentationLibraryInfo instrumentationLibraryInfo) {
      this.instrumentationLibraryInfo = instrumentationLibraryInfo;
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

    public Builder setName(String name) {
      this.name = name;
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

    public MockLogData build() {
      return new MockLogData(this);
    }
  }
}
