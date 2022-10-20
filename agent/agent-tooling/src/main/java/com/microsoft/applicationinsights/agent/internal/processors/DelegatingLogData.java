// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

public abstract class DelegatingLogData implements LogRecordData {

  private final LogRecordData delegate;

  protected DelegatingLogData(LogRecordData delegate) {
    this.delegate = requireNonNull(delegate, "delegate");
  }

  @Override
  public Resource getResource() {
    return delegate.getResource();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return delegate.getInstrumentationScopeInfo();
  }

  @Override
  public long getEpochNanos() {
    return delegate.getEpochNanos();
  }

  @Override
  public SpanContext getSpanContext() {
    return delegate.getSpanContext();
  }

  @Override
  public Severity getSeverity() {
    return delegate.getSeverity();
  }

  @Nullable
  @Override
  public String getSeverityText() {
    return delegate.getSeverityText();
  }

  @Override
  public Body getBody() {
    return delegate.getBody();
  }

  @Override
  public Attributes getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public int getTotalAttributeCount() {
    return delegate.getTotalAttributeCount();
  }

  @Override
  public String toString() {
    return "DelegatingLogData{"
        + "spanContext="
        + getSpanContext()
        + ", "
        + "resource="
        + getResource()
        + ", "
        + "instrumentationScopeInfo="
        + getInstrumentationScopeInfo()
        + ", "
        + "epochNanos="
        + getEpochNanos()
        + ", "
        + "attributes="
        + getAttributes()
        + ", "
        + "severity="
        + getSeverity()
        + ", "
        + "severityText="
        + getSeverityText()
        + ", "
        + "body="
        + getBody()
        + "}";
  }
}
