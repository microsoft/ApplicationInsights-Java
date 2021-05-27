package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.processors.AgentProcessor.IncludeExclude;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExporterWithLogProcessor implements SpanExporter {

    private final SpanExporter delegate;
    private final SpanProcessor logProcessor;

    // caller should check config.isValid before creating
    public ExporterWithLogProcessor(ProcessorConfig config, SpanExporter delegate) throws FriendlyException {
        config.validate();
        logProcessor = SpanProcessor.create(config);
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
        IncludeExclude include = logProcessor.getInclude();
        if(!ProcessorUtil.isSpanOfTypeLog(span)) {
            return span;
        }
        if (include != null && !include.isMatch(span, true)) {
            //If Not included we can skip further processing
            return span;
        }
        IncludeExclude exclude = logProcessor.getExclude();
        if (exclude != null && exclude.isMatch(span, true)) {
            return span;
        }

        SpanData updatedSpan = logProcessor.processFromAttributes(span);
        return logProcessor.processToAttributes(updatedSpan);
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
