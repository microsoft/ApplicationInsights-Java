package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.processors.AgentProcessor.IncludeExclude;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ExporterWithAttributeProcessor implements SpanExporter {

    private final SpanExporter delegate;
    private final AttributeProcessor attributeProcessor;

    // caller should check config.isValid before creating
    public ExporterWithAttributeProcessor(ProcessorConfig config, SpanExporter delegate) throws FriendlyException {
        config.validate();
        attributeProcessor = AttributeProcessor.create(config);
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // we need to filter attributes before passing on to delegate
            List<SpanData> copy = new ArrayList<>();
            for (SpanData span : spans) {
                copy.add(process(span));
            }
            return delegate.export(copy);
    }

    private SpanData process(SpanData span) {
        IncludeExclude include = attributeProcessor.getInclude();
        if (include != null && !include.isMatch(span)) {
            //If Not included we can skip further processing
            return span;
        }
        IncludeExclude exclude = attributeProcessor.getExclude();
        if (exclude != null && exclude.isMatch(span)) {
            return span;
        }
        return attributeProcessor.processActions(span);
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
