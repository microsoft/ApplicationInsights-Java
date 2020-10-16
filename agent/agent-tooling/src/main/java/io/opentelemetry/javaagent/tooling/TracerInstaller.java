package io.opentelemetry.javaagent.tooling;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.sampling.FixedRateSampler;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.instrumentation.api.aiappid.AiHttpTraceContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class TracerInstaller {

    public static void installAgentTracer() {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        InstrumentationSettings config = MainEntryPoint.getConfiguration();
        Map<String, SpanProcessorConfig> spanProcessors = config.preview.spanProcessors;
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        OpenTelemetry.setPropagators(
                DefaultContextPropagators.builder().addTextMapPropagator(new AiHttpTraceContext()).build());

        double fixedRateSamplingPercentage = Global.getFixedRateSamplingPercentage();
        if (fixedRateSamplingPercentage != 100) {
            OpenTelemetrySdk.getTracerProvider().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(new FixedRateSampler(fixedRateSamplingPercentage))
                            .build());
        }
        // if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient

        if (!spanProcessors.isEmpty()) {
            ExporterWithSpanProcessor currExporterWithSpanProcessor = null;
            ExporterWithSpanProcessor prevExporterWithSpanProcessor = null;
            for (Map.Entry<String, SpanProcessorConfig> spanProcessorConfigEntry : spanProcessors.entrySet()) {
                SpanProcessorConfig spanProcessorConfig = spanProcessorConfigEntry.getValue();
                if (prevExporterWithSpanProcessor == null) {
                    currExporterWithSpanProcessor = new ExporterWithSpanProcessor(spanProcessorConfig, new Exporter(telemetryClient));

                } else {
                    currExporterWithSpanProcessor = new ExporterWithSpanProcessor(spanProcessorConfig, prevExporterWithSpanProcessor);
                }
                prevExporterWithSpanProcessor = currExporterWithSpanProcessor;
            }

            OpenTelemetrySdk.getTracerProvider().addSpanProcessor(SimpleSpanProcessor.newBuilder(currExporterWithSpanProcessor).build());

        } else {
            OpenTelemetrySdk.getTracerProvider()
                    .addSpanProcessor(SimpleSpanProcessor.newBuilder(new Exporter(telemetryClient)).build());
        }
    }

    public static void logVersionInfo() {
    }
}
