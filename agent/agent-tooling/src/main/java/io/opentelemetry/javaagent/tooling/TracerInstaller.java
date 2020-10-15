package io.opentelemetry.javaagent.tooling;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.FixedRateSampler;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.instrumentation.api.aiappid.AiHttpTraceContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.Samplers;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class TracerInstaller {

    public static void installAgentTracer() {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
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
        } else {
            // TODO will revisit to delete DefaultSpan
            // For the case of AzureFunctionInstrumentation, TraceParent comes in this format: 00-ea5a92ef3cf03649acf1b89c2b5d4211-b70350d129319a4b-00
            // Sampling is always off.  We need to use AlwaysOnSampler.
            OpenTelemetrySdk.getTracerProvider().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(Samplers.alwaysOn())
                            .build());
        }
        // if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient
        OpenTelemetrySdk.getTracerProvider()
                .addSpanProcessor(SimpleSpanProcessor.newBuilder(new Exporter(telemetryClient)).build());
    }

    public static void logVersionInfo() {
    }
}
