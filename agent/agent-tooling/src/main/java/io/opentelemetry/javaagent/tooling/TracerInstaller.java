package io.opentelemetry.javaagent.tooling;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.bootstrap.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.AppIdSupplier;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.instrumentation.api.aiappid.AiHttpTraceContext;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkManagement;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class TracerInstaller {

    public static void installAgentTracer() throws FriendlyException {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        // only safe now to resolve app id because SSL initialization
        // triggers loading of java.util.logging (starting with Java 8u231)
        // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
        AppIdSupplier.registerAndTriggerResolution();

        Configuration config = MainEntryPoint.getConfiguration();
        List<ProcessorConfig> processors = new ArrayList<>(config.preview.processors);
        // Reversing the order of processors before passing it to SpanProcessor
        Collections.reverse(processors);

        if (config.connectionString != null) {
            setGlobalPropagators(
                    DefaultContextPropagators.builder().addTextMapPropagator(AiHttpTraceContext.getInstance()).build());
        } else {
            // in Azure Functions, need to set lazy once we know user has opted in to tracing
            setGlobalPropagators(DefaultContextPropagators.builder().build());
        }

        TracerSdkManagement tracerManagement = OpenTelemetrySdk.getGlobalTracerManagement();

        // Register additional thread details
        tracerManagement.addSpanProcessor(new AddThreadDetailsSpanProcessor());

        tracerManagement.updateActiveTraceConfig(
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

            tracerManagement.addSpanProcessor(SimpleSpanProcessor.builder(currExporter).build());

        } else {
            tracerManagement.addSpanProcessor(SimpleSpanProcessor.builder(new Exporter(telemetryClient)).build());
        }
    }

    // Workaround https://github.com/open-telemetry/opentelemetry-java/pull/2096
    public static void setGlobalPropagators(ContextPropagators propagators) {
        OpenTelemetry.set(
                OpenTelemetrySdk.builder()
                        .setResource(OpenTelemetrySdk.get().getResource())
                        .setClock(OpenTelemetrySdk.get().getClock())
                        .setMeterProvider(OpenTelemetry.getGlobalMeterProvider())
                        .setTracerProvider(unobfuscate(OpenTelemetry.getGlobalTracerProvider()))
                        .setPropagators(propagators)
                        .build());
    }

    private static TracerProvider unobfuscate(TracerProvider tracerProvider) {
        if (tracerProvider.getClass().getName().endsWith("TracerSdkProvider")) {
            return tracerProvider;
        }
        try {
            Method unobfuscate = tracerProvider.getClass().getDeclaredMethod("unobfuscate");
            unobfuscate.setAccessible(true);
            return (TracerProvider) unobfuscate.invoke(tracerProvider);
        } catch (Throwable t) {
            return tracerProvider;
        }
    }

    public static void logVersionInfo() {
    }
}
