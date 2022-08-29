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

package com.microsoft.applicationinsights.agent.internal;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;

public class SamplingTestUtil {

  public static double getCurrentSamplingPercentage(Sampler sampler) {
    SpanContext spanContext =
        SpanContext.createFromRemoteParent(
            "12341234123412341234123412341234",
            "1234123412341234",
            TraceFlags.getSampled(),
            TraceState.getDefault());
    Context parentContext = Context.root().with(Span.wrap(spanContext));
    SamplingResult samplingResult =
        sampler.shouldSample(
            parentContext,
            // traceId=27272727272727272727272727272727 is known to produce a score of 0.66 (out
            // of 100) so will be sampled as long as samplingPercentage > 1%
            "27272727272727272727272727272727",
            "my span name",
            SpanKind.SERVER,
            Attributes.empty(),
            Collections.emptyList());
    Long itemCount = samplingResult.getAttributes().get(AiSemanticAttributes.ITEM_COUNT);
    return 100.0 / itemCount;
  }

  private SamplingTestUtil() {}
}
