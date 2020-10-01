package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class MockExporter implements SpanExporter {

    private final List<SpanData> spans = new ArrayList<>();

    public List<SpanData> getSpans() {
        return spans;
    }

    @Override
    public ResultCode export(Collection<SpanData> spans) {
        this.spans.addAll(spans);
        return ResultCode.SUCCESS;
    }

    @Override
    public ResultCode flush() {
        return ResultCode.SUCCESS;
    }

    @Override
    public void shutdown() {
    }
}
