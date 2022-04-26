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

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class TelemetryClientFlushingSpanProcessor implements SpanProcessor {

  private final SpanProcessor delegate;
  private final TelemetryClient telemetryClient;

  public TelemetryClientFlushingSpanProcessor(
      SpanProcessor delegate, TelemetryClient telemetryClient) {
    this.delegate = delegate;
    this.telemetryClient = telemetryClient;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    delegate.onStart(parentContext, span);
  }

  @Override
  public boolean isStartRequired() {
    return delegate.isStartRequired();
  }

  @Override
  public void onEnd(ReadableSpan span) {
    delegate.onEnd(span);
  }

  @Override
  public boolean isEndRequired() {
    return delegate.isEndRequired();
  }

  @Override
  public CompletableResultCode shutdown() {
    // see https://github.com/open-telemetry/opentelemetry-java/issues/4416
    return forceFlush();
  }

  @Override
  public CompletableResultCode forceFlush() {
    CompletableResultCode overallResult = new CompletableResultCode();
    CompletableResultCode delegateResult = delegate.forceFlush();
    delegateResult.whenComplete(
        () -> {
          if (delegateResult.isSuccess()) {
            CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
            telemetryClientResult.whenComplete(
                () -> {
                  if (telemetryClientResult.isSuccess()) {
                    overallResult.succeed();
                  } else {
                    overallResult.fail();
                  }
                });
          } else {
            overallResult.fail();
          }
        });
    return overallResult;
  }

  @Override
  public void close() {
    delegate.close();
  }
}
