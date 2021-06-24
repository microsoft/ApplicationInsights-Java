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
import io.opentelemetry.sdk.extension.incubator.trace.data.DelegatingSpanData;
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
