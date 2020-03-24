/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.applicationinsights.agent.internal.bootstrap;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;

import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.diagnostics.status.StatusFile;
import io.opentelemetry.auto.bootstrap.Agent;
import io.opentelemetry.auto.bootstrap.ConfigureLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// this class has one purpose, start diagnostics before passing control to opentelemetry-auto-instr-java
public class MainEntryPoint {

    private MainEntryPoint() {
    }

    public static void start(Instrumentation instrumentation, URL bootstrapURL) {
        boolean success = false;
        Logger startupLogger = null;
        try {
            File agentJarFile = new File(bootstrapURL.toURI());
            DiagnosticsHelper.setAgentJarFile(agentJarFile);
            MDC.put("microsoft.ai.operationName", "Startup");
            startupLogger = configureLogging();
            Agent.start(instrumentation, bootstrapURL);
            success = true;
            LoggerFactory.getLogger(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME)
                    .info("Application Insights Codeless Agent Attach Successful");
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            if (startupLogger != null) {
                startupLogger.error("Agent failed to start.", t);
            } else {
                t.printStackTrace();
            }
        } finally {
            try {
                StatusFile.putValueAndWrite("AgentInitializedSuccessfully", success);
            } catch (Exception e) {
                startupLogger.error("Error writing status.json", e);
            }
            MDC.clear();
        }
    }

    private static Logger configureLogging() {
        if (DiagnosticsHelper.isAppServiceCodeless()) {
            ClassLoader cl = ConfigureLogging.class.getClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            final URL appsvcConfig = cl.getResource("appsvc.ai.logback.xml");
            System.setProperty("ai.logback.configurationFile", appsvcConfig.toString());
        }

        try {
            return LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");
        } finally {
            System.clearProperty("ai.logback.configurationFile");
        }
    }
}
