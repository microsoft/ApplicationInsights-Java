package io.opentelemetry.auto.bootstrap;

import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import org.slf4j.LoggerFactory;

public class ConfigureLogging {

    public static void configure() {
        if (DiagnosticsHelper.isAppServiceCodeless()) {
            ClassLoader cl = ConfigureLogging.class.getClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            final URL appsvcConfig = cl.getResource("appsvc.ai.logback.xml");
            System.setProperty("ai.logback.configurationFile", appsvcConfig.toString());
        }

        try {
            // init slf4j/logback
            LoggerFactory.getLogger(ConfigureLogging.class);
        } finally {
            System.clearProperty("ai.logback.configurationFile");
        }
    }
}
