package io.opentelemetry.auto.tooling;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.MainEntryPoint;

public class BeforeAgentInstaller {

    public static void beforeInstallBytebuddyAgent(Instrumentation inst, URL bootstrapURL) throws URISyntaxException {
        File javaagentFile = new File(bootstrapURL.toURI());
        MainEntryPoint.premain(inst, javaagentFile);
    }
}
