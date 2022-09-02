// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;

public class MySpanData extends DelegatingSpanData {
  private final Attributes attributes;
  private final String spanName;

  public MySpanData(SpanData delegate, Attributes attributes) {
    this(delegate, attributes, delegate.getName());
  }

  public MySpanData(SpanData delegate, Attributes attributes, String spanName) {
    super(delegate);
    this.attributes = attributes;
    this.spanName = spanName;
  }

  @Override
  public String getName() {
    return spanName;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }
}
