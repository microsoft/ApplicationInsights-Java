// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
