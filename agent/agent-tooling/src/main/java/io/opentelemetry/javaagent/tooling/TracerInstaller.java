package io.opentelemetry.javaagent.tooling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.instrumentation.api.aiappid.AiHttpTraceContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class TracerInstaller {

    public static void installAgentTracer() {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        Configuration config = MainEntryPoint.getConfiguration();
        List<ProcessorConfig> processors = new ArrayList<>(config.preview.processors);
        // Reversing the order of processors before passing it to SpanProcessor
        Collections.reverse(processors);
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        OpenTelemetry.setGlobalPropagators(
                DefaultContextPropagators.builder().addTextMapPropagator(AiHttpTraceContext.getInstance()).build());

        OpenTelemetrySdk.getGlobalTracerManagement().updateActiveTraceConfig(
                TraceConfig.getDefault().toBuilder()
                        .setSampler(Samplers.getSampler(Global.getSamplingPercentage()))
                        .build());
        // if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient
        if (!processors.isEmpty()) {
            SpanExporter currExporter = null;
            for (ProcessorConfig processorConfig : processors) {

                if (currExporter == null) {
                    currExporter = processorConfig.type == ProcessorType.attribute ?
                            new ExporterWithAttributeProcessor(processorConfig, new Exporter(telemetryClient)) :
                            new ExporterWithSpanProcessor(processorConfig, new Exporter(telemetryClient));

                } else {
                    currExporter = processorConfig.type == ProcessorType.attribute ?
                            new ExporterWithAttributeProcessor(processorConfig, currExporter) :
                            new ExporterWithSpanProcessor(processorConfig, currExporter);
                }
            }

            OpenTelemetrySdk.getGlobalTracerManagement().addSpanProcessor(SimpleSpanProcessor.builder(currExporter).build());

        } else {
            OpenTelemetrySdk.getGlobalTracerManagement()
                    .addSpanProcessor(SimpleSpanProcessor.builder(new Exporter(telemetryClient)).build());
        }
    }

    public static void logVersionInfo() {
    }
}
