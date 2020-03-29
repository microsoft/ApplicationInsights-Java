package io.opentelemetry.auto.tooling;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.FixedRateSampler;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.auto.bootstrap.instrumentation.aiappid.AiHttpTraceContext;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;

public class TracerInstaller {

    public static void installAgentTracer() {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        OpenTelemetry.setPropagators(
                DefaultContextPropagators.builder().addHttpTextFormat(new AiHttpTraceContext()).build());

        double fixedRateSamplingPercentage = Global.getFixedRateSamplingPercentage();
        if (fixedRateSamplingPercentage != 100) {
            OpenTelemetrySdk.getTracerProvider().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(new FixedRateSampler(fixedRateSamplingPercentage))
                            .build());
        }
        OpenTelemetrySdk.getTracerProvider()
                .addSpanProcessor(SimpleSpansProcessor.newBuilder(new Exporter(telemetryClient)).build());
    }

    public static void logVersionInfo() {
    }
}
