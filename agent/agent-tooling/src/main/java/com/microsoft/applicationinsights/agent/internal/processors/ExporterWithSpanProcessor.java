package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ExporterWithSpanProcessor implements SpanExporter {

    private final SpanExporter delegate;
    private final SpanProcessor spanProcessor;

    public ExporterWithSpanProcessor(SpanProcessorConfig config, SpanExporter delegate) {
        this.spanProcessor = SpanProcessor.create(config);
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // we need to filter attributes before passing on to delegate
        if (this.spanProcessor.hasValidConfig()) {
            List<SpanData> copy = new ArrayList<>();
            for (SpanData span : spans) {
                copy.add(process(span));
            }
            return delegate.export(copy);
        } else {
            return delegate.export(spans);
        }
    }

    private SpanData process(SpanData span) {
        boolean includeFlag = true;//Flag to check if the span is included in processing
        boolean excludeFlag = false;//Flag to check if the span is excluded in processing
        if(spanProcessor.getInclude()!=null) {
            includeFlag = spanProcessor.getInclude().isMatch(span);
        }
        if (!includeFlag) return span;//If Not included we can skip further processing
        if(spanProcessor.getExclude()!=null) {
            excludeFlag = spanProcessor.getExclude().isMatch(span);
        }
        if (includeFlag && !excludeFlag) {
            SpanData updatedSpan = spanProcessor.processOtherActions(span);
            return spanProcessor.processInsertActions(updatedSpan);
        }
        return span;
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
