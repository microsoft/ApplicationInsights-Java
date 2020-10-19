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
            // OpenTelemetry default sampling is "parent based", which means don't sample if remote traceparent sampled flag was not set,
            // and Azure Functions is not setting the sampled flag on traceparent currently, so we can't use the default currently, and instead default to "always on" in this case
            // TODO revisit using "parent based" both for 100% and fixed-rate sampler above
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
