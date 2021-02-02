package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.agent.Exporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class OperationNameSpanProcessor implements SpanProcessor {

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        Span serverSpan = BaseTracer.getCurrentServerSpan(parentContext);
        if (serverSpan instanceof ReadableSpan) {
            span.setAttribute(Exporter.AI_OPERATION_NAME_KEY, ((ReadableSpan) serverSpan).getName());
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
