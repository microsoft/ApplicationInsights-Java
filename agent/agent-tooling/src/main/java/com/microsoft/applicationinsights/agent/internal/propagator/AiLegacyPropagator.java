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

package com.microsoft.applicationinsights.agent.internal.propagator;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.LegacyHeaderSpanProcessor.LegacyIds;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
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
    String appId = AiAppId.getAppId();
    if (!appId.isEmpty()) {
      setter.set(carrier, "Request-Context", "appId=" + appId);
    }
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
  private static String generateSpanId() {
    long id;
    ThreadLocalRandom random = ThreadLocalRandom.current();
    do {
      id = random.nextLong();
    } while (id == INVALID_ID);
    return SpanId.fromLong(id);
  }

  // copied from io.opentelemetry.sdk.trace.RandomIdGenerator
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
