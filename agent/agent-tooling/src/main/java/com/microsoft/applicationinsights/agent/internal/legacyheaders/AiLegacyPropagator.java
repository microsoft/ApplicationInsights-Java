// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.legacyheaders;

import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyHeaderSpanProcessor.LegacyIds;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;

// this propagator handles the legacy Application Insights distributed tracing header format
public class AiLegacyPropagator implements TextMapPropagator {

  private static final TextMapPropagator instance = new AiLegacyPropagator();

  public static TextMapPropagator getInstance() {
    return instance;
  }

  private AiLegacyPropagator() {}

  @Override
  public Collection<String> fields() {
    return Arrays.asList("Request-Id", "Request-Context");
  }

  @Override
  public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (!spanContext.isValid()) {
      return;
    }
    setter.set(carrier, "Request-Id", getRequestId(spanContext));
  }

  @Override
  public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {

    // see behavior specified at
    // https://github.com/microsoft/ApplicationInsights-Java/issues/1174

    if (getter.get(carrier, "traceparent") != null) {
      // no need to handle legacy format
      return context;
    }

    String legacyParentId = getter.get(carrier, "Request-Id");
    if (legacyParentId == null || legacyParentId.isEmpty()) {
      return context;
    }

    String legacyRootId = aiExtractRootId(legacyParentId);
    String traceId;
    if (TraceId.isValid(legacyRootId)) {
      traceId = legacyRootId;
      legacyRootId = null; // clear it out, because we don't need to create span attribute for it
    } else {
      traceId = generateTraceId();
    }

    // have to generate a random spanId, and we will patch the real legacyParentId back in during
    // export
    String spanId = generateSpanId();
    // there are no flags, so we assume sampled
    SpanContext spanContext =
        SpanContext.createFromRemoteParent(
            traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault());

    return context
        .with(new LegacyIds(spanContext, legacyParentId, legacyRootId))
        .with(Span.wrap(spanContext));
  }

  private static String getRequestId(SpanContext spanContext) {
    return '|' + spanContext.getTraceId() + '.' + spanContext.getSpanId() + '.';
  }

  private static String aiExtractRootId(String parentId) {
    // ported from .NET's System.Diagnostics.Activity.cs implementation:
    // https://github.com/dotnet/corefx/blob/master/src/System.Diagnostics.DiagnosticSource/src/System/Diagnostics/Activity.cs

    int rootEnd = parentId.indexOf('.');
    if (rootEnd < 0) {
      rootEnd = parentId.length();
    }

    int rootStart = parentId.charAt(0) == '|' ? 1 : 0;

    return parentId.substring(rootStart, rootEnd);
  }

  private static final long INVALID_ID = 0;

  // copied from io.opentelemetry.sdk.trace.RandomIdGenerator
  @SuppressFBWarnings(
      value = "SECPR", // Predictable pseudorandom number generator
      justification = "Predictable random is ok for span id")
  public static String generateSpanId() {
    long id;
    ThreadLocalRandom random = ThreadLocalRandom.current();
    do {
      id = random.nextLong();
    } while (id == INVALID_ID);
    return SpanId.fromLong(id);
  }

  // copied from io.opentelemetry.sdk.trace.RandomIdGenerator
  @SuppressFBWarnings(
      value = "SECPR", // Predictable pseudorandom number generator
      justification = "Predictable random is ok for trace id")
  private static String generateTraceId() {
    long idHi;
    long idLo;
    ThreadLocalRandom random = ThreadLocalRandom.current();
    do {
      idHi = random.nextLong();
      idLo = random.nextLong();
    } while (idHi == INVALID_ID && idLo == INVALID_ID);
    return TraceId.fromLongs(idHi, idLo);
  }
}
