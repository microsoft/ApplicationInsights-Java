package io.opentelemetry.javaagent.tooling;

import java.lang.instrument.Instrumentation;
import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.AiComponentInstaller;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.MainEntryPoint;

public class AgentInstallerOverride {

    public static void installBytebuddyAgent(Instrumentation inst, URL bootstrapURL) {
        MainEntryPoint.start(inst, bootstrapURL);
    }
}
