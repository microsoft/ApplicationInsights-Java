package io.opentelemetry.javaagent.tooling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.sampling.TraceIdBasedSampler;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.instrumentation.api.aiappid.AiHttpTraceContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.Samplers;
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

        OpenTelemetry.setPropagators(
                DefaultContextPropagators.builder().addTextMapPropagator(AiHttpTraceContext.getInstance()).build());

        double samplingPercentage = Global.getSamplingPercentage();
        if (samplingPercentage != 1) {
            OpenTelemetrySdk.getTracerManagement().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(new TraceIdBasedSampler(samplingPercentage))
                            .build());
        } else {
            // OpenTelemetry default sampling is "parent based", which means don't sample if remote traceparent sampled flag was not set,
            // but Application Insights SDKs do not send the sampled flag (since they perform sampling during export instead of head-based sampling)
            // so need to use "always on" in this case
            OpenTelemetrySdk.getTracerManagement().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(Samplers.alwaysOn())
                            .build());
        }
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

            OpenTelemetrySdk.getTracerManagement().addSpanProcessor(SimpleSpanProcessor.newBuilder(currExporter).build());

        } else {
            OpenTelemetrySdk.getTracerManagement()
                    .addSpanProcessor(SimpleSpanProcessor.newBuilder(new Exporter(telemetryClient)).build());
        }
    }

    public static void logVersionInfo() {
    }
}
