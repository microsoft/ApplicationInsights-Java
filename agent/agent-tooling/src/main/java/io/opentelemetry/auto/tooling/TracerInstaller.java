package io.opentelemetry.auto.tooling;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.Global;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;

public class TracerInstaller {

    public static void installAgentTracer() {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }
        OpenTelemetrySdk.getTracerFactory()
                .addSpanProcessor(SimpleSpansProcessor.newBuilder(new Exporter(telemetryClient)).build());
    }

    public static void logVersionInfo() {
    }
}
