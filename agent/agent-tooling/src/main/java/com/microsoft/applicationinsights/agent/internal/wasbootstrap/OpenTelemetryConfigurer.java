package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.propagator.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class OpenTelemetryConfigurer implements SdkTracerProviderConfigurer {

    @Override
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

        // FIXME (trask) pass in config.preview.httpMethodInOperationName
        SpanExporter exporter = new AzureMonitorExporterBuilder()
                .connectionString(TelemetryConfiguration.getActive().getConnectionString())
                .buildTraceExporter();

        // NOTE if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient
        if (!processors.isEmpty()) {
            SpanExporter currExporter = null;
            for (ProcessorConfig processorConfig : processors) {

                if (currExporter == null) {
                    currExporter = processorConfig.type == ProcessorType.attribute ?
                            new ExporterWithAttributeProcessor(processorConfig, exporter) :
                            new ExporterWithSpanProcessor(processorConfig, exporter);

                } else {
                    currExporter = processorConfig.type == ProcessorType.attribute ?
                            new ExporterWithAttributeProcessor(processorConfig, currExporter) :
                            new ExporterWithSpanProcessor(processorConfig, currExporter);
                }
            }

            tracerProvider.addSpanProcessor(SimpleSpanProcessor.create(currExporter));

        } else {
            tracerProvider.addSpanProcessor(SimpleSpanProcessor.create(exporter));
        }
    }
}
