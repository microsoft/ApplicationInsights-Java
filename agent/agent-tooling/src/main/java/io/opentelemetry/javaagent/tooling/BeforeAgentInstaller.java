package io.opentelemetry.javaagent.tooling;

import java.lang.instrument.Instrumentation;
import java.net.URL;

public class BeforeAgentInstaller {

    public static void beforeInstallBytebuddyAgent(Instrumentation inst, URL bootstrapURL) throws Exception {
        com.microsoft.applicationinsights.agent.internal.BeforeAgentInstaller.beforeInstallBytebuddyAgent(inst, bootstrapURL);
    }
}
