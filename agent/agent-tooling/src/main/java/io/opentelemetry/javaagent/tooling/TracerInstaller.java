package io.opentelemetry.javaagent.tooling;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.TraceIdBasedSampler;
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
                DefaultContextPropagators.builder().addTextMapPropagator(AiHttpTraceContext.getInstance()).build());

        double samplingProbability = Global.getSamplingProbability();
        if (samplingProbability != 1) {
            OpenTelemetrySdk.getTracerManagement().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(new TraceIdBasedSampler(samplingProbability))
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
        OpenTelemetrySdk.getTracerManagement()
                .addSpanProcessor(SimpleSpanProcessor.newBuilder(new Exporter(telemetryClient)).build());
    }

    public static void logVersionInfo() {
    }
}
