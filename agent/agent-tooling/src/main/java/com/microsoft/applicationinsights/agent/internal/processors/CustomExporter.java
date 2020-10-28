package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.Collection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public abstract class CustomExporter implements SpanExporter {

    @Override public abstract CompletableResultCode export(Collection<SpanData> spans);

    @Override public abstract CompletableResultCode flush();

    @Override public abstract CompletableResultCode shutdown();
}
