package io.opentelemetry.javaagent.tooling;

import java.lang.instrument.Instrumentation;

import com.microsoft.applicationinsights.agent.internal.AppIdSupplier;
import io.opentelemetry.javaagent.spi.ComponentInstaller;

public class BeforeAgentInstaller implements ComponentInstaller {

    private final OpenTelemetryInstaller openTelemetryInstaller = new OpenTelemetryInstaller();

    // called via reflection hack added to otel-fork
    public static void beforeInstallBytebuddyAgent(Instrumentation inst) throws Exception {
        com.microsoft.applicationinsights.agent.internal.BeforeAgentInstaller.beforeInstallBytebuddyAgent(inst);
    }

    @Override
    public void beforeByteBuddyAgent() {
        openTelemetryInstaller.beforeByteBuddyAgent();
    }

    @Override
    public void afterByteBuddyAgent() {
        openTelemetryInstaller.afterByteBuddyAgent();

        // only safe now to resolve app id because SSL initialization
        // triggers loading of java.util.logging (starting with Java 8u231)
        // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
        AppIdSupplier.registerAndTriggerResolution();
    }
}
