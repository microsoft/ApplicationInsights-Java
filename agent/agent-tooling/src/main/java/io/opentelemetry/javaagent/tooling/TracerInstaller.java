package io.opentelemetry.javaagent.tooling;

import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
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
        final InstrumentationSettings config = MainEntryPoint.getConfiguration();
        final List<ProcessorConfig> spanProcessors = config.preview.processors.stream().filter(processorConfig -> processorConfig.type == ProcessorType.attribute).collect(Collectors.toList());
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        OpenTelemetry.setPropagators(
                DefaultContextPropagators.builder().addTextMapPropagator(AiHttpTraceContext.getInstance()).build());

        double fixedRateSamplingPercentage = Global.getFixedRateSamplingPercentage();
        if (fixedRateSamplingPercentage != 100) {
            OpenTelemetrySdk.getTracerManagement().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(new FixedRateSampler(fixedRateSamplingPercentage))
                            .build());
        } else {
            // OpenTelemetry default sampling is "parent based", which means don't sample if remote traceparent sampled flag was not set,
            // and Azure Functions is not setting the sampled flag on traceparent currently, so we can't use the default currently, and instead default to "always on" in this case
            // TODO revisit using "parent based" both for 100% and fixed-rate sampler above
            OpenTelemetrySdk.getTracerManagement().updateActiveTraceConfig(
                    TraceConfig.getDefault().toBuilder()
                            .setSampler(Samplers.alwaysOn())
                            .build());
        }
        // if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient
        if (!spanProcessors.isEmpty()) {
            ExporterWithAttributeProcessor currExporterWithAttributeProcessor = null;
            ExporterWithAttributeProcessor prevExporterWithAttributeProcessor = null;
            for (ProcessorConfig processorConfig : spanProcessors) {
                if (prevExporterWithAttributeProcessor == null) {
                    currExporterWithAttributeProcessor = new ExporterWithAttributeProcessor(processorConfig, new Exporter(telemetryClient));

                } else {
                    currExporterWithAttributeProcessor = new ExporterWithAttributeProcessor(processorConfig, prevExporterWithAttributeProcessor);
                }
                prevExporterWithAttributeProcessor = currExporterWithAttributeProcessor;
            }

            OpenTelemetrySdk.getTracerManagement().addSpanProcessor(SimpleSpanProcessor.newBuilder(currExporterWithAttributeProcessor).build());

        } else {
            OpenTelemetrySdk.getTracerManagement()
                    .addSpanProcessor(SimpleSpanProcessor.newBuilder(new Exporter(telemetryClient)).build());
        }
    }

    public static void logVersionInfo() {
    }
}
