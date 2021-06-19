package io.opentelemetry.javaagent.tooling;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.MainEntryPoint;

public class AgentInstallerOverride {

    public static void installBytebuddyAgent(Instrumentation inst, File javaagentFile) {
        MainEntryPoint.start(inst, javaagentFile);
    }
}
