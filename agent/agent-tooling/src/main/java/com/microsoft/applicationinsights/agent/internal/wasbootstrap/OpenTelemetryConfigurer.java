package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.propagator.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenTelemetryConfigurer implements SdkTracerProviderConfigurer {

    private static volatile BatchSpanProcessor batchSpanProcessor;

    public static CompletableResultCode flush() {
        return batchSpanProcessor.forceFlush();
    }

    @Override
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
                        justification = "this method is only called once during initialization")
    public void configure(SdkTracerProviderBuilder tracerProvider) {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        Configuration config = MainEntryPoint.getConfiguration();

        tracerProvider.setSampler(DelegatingSampler.getInstance());

        if (config.connectionString != null) {
            DelegatingPropagator.getInstance().setUpStandardDelegate();
            DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(config.sampling.percentage, config));
        } else {
            // in Azure Functions, we configure later on, once we know user has opted in to tracing
            // (note: the default for DelegatingPropagator is to not propagate anything
            // and the default for DelegatingSampler is to not sample anything)
        }

        List<ProcessorConfig> processors = new ArrayList<>(config.preview.processors);
        // Reversing the order of processors before passing it to SpanProcessor
        Collections.reverse(processors);

        SpanExporter currExporter = new Exporter(TelemetryClient.getActive());

        // NOTE if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient
        if (!processors.isEmpty()) {
            for (ProcessorConfig processorConfig : processors) {
                if (processorConfig.type != null) { // Added this condition to resolve spotbugs NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD issue
                    switch (processorConfig.type) {
                        case attribute:
                            currExporter = new ExporterWithAttributeProcessor(processorConfig, currExporter);
                            break;
                        case span:
                            currExporter = new ExporterWithSpanProcessor(processorConfig, currExporter);
                            break;
                        case log:
                            currExporter = new ExporterWithLogProcessor(processorConfig, currExporter);
                            break;
                        default:
                            throw new IllegalStateException("Not an expected ProcessorType: " + processorConfig.type);
                    }
                }
            }
        }

        // using BatchSpanProcessor in order to get off of the application thread as soon as possible
        // using batch size 1 because need to convert to SpanData as soon as possible to grab data for live metrics
        // real batching is done at a lower level
        batchSpanProcessor = BatchSpanProcessor.builder(currExporter)
                .setMaxExportBatchSize(1)
                .build();
        tracerProvider.addSpanProcessor(batchSpanProcessor);
    }
}
