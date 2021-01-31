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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.aiappid.AiAppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiBackCompatPropagator implements TextMapPropagator {

    private static final Logger logger = LoggerFactory.getLogger(AiBackCompatPropagator.class.getName());

    private static final TextMapPropagator instance = new AiBackCompatPropagator();

    private static final int TRACE_ID_HEX_SIZE = TraceId.getHexLength();
    private static final int SPAN_ID_HEX_SIZE = SpanId.getHexLength();

    public static TextMapPropagator getInstance() {
        return instance;
    }

    private AiBackCompatPropagator() {
    }

    @Override
    public Collection<String> fields() {
        return Arrays.asList("Request-Id", "Request-Context");
    }

    @Override
    public <C> void inject(Context context, @Nullable C carrier, Setter<C> setter) {
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
    public <C> Context extract(Context context, @Nullable C carrier, Getter<C> getter) {

        String aiRequestId = getter.get(carrier, "Request-Id");
        if (aiRequestId == null || aiRequestId.isEmpty()) {
            return context;
        }

        // see behavior specified at
        // https://github.com/microsoft/ApplicationInsights-Java/issues/1174
        String legacyOperationId = aiExtractRootId(aiRequestId);
        TraceStateBuilder traceState =
                TraceState.builder().set("ai-legacy-parent-id", aiRequestId);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String traceIdHex;
        try {
            traceIdHex = legacyOperationId;
        } catch (IllegalArgumentException e) {
            logger.info("Request-Id root part is not compatible with trace-id.");
            // see behavior specified at
            // https://github.com/microsoft/ApplicationInsights-Java/issues/1174
            traceIdHex = TraceId.fromLongs(random.nextLong(), random.nextLong());
            traceState.set("ai-legacy-operation-id", legacyOperationId);
        }
        String spanIdHex = SpanId.fromLong(random.nextLong());
        byte traceFlags = TraceFlags.getDefault();
        SpanContext spanContext = SpanContext.createFromRemoteParent(
                traceIdHex, spanIdHex, traceFlags, traceState.build());

        if (!spanContext.isValid()) {
            return context;
        }

        return context.with(Span.wrap(spanContext));
    }

    private static String getRequestId(SpanContext spanContext) {
        StringBuilder requestId = new StringBuilder(TRACE_ID_HEX_SIZE + SPAN_ID_HEX_SIZE + 3);
        requestId.append('|');
        requestId.append(spanContext.getTraceIdAsHexString());
        requestId.append('.');
        requestId.append(spanContext.getSpanIdAsHexString());
        requestId.append('.');
        return requestId.toString();
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
}
