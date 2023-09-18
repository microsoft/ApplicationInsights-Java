// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

// IMPORTANT!! If this class is renamed, be sure to add the previous name to
// DuplicateAgentClassFileTransformer
// so that previous versions will be suppressed (current versions with the same class name are
// suppressed
// below via the alreadyLoaded flag
public class Agent {

  // this is to prevent the agent from loading and instrumenting everything twice
  // (leading to unpredictable results) when -javaagent:applicationinsights-agent.jar
  // appears multiple times on the command line
  private static final AtomicBoolean alreadyLoaded = new AtomicBoolean(false);

  public static void premain(String agentArgs, Instrumentation inst) {
    if (alreadyLoaded.getAndSet(true)) {
      return;
    }

    if (Boolean.getBoolean("applicationinsights.debug.startupProfiling")) {
      StartupProfiler.start();
    }

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7339
    // Starting 1.25.0, OpenTelemetry defaults logger to Slf4jSimplerlogger when system property
    // "otel.javaagent.logging" is null.
    // Setting this system property programmatically will tell agent class loader to load our
    // Slf4jInternalLogger instead.
    System.setProperty("otel.javaagent.logging", "applicationinsights");

    OpenTelemetryAgent.premain(agentArgs, inst);
  }

  // this is provided only for dynamic attach in the first line of main
  // there are many problematic edge cases around dynamic attach any later than that
  public static void agentmain(String agentArgs, Instrumentation inst) {
    premain(agentArgs, inst);
  }

  @SuppressWarnings("SystemOut")
  public static void main(String... args) {
    System.err.println(
        "*************************\n"
            + "Application Insights Java agent should be attached to an existing Java application"
            + " using the -javaagent flag.\n"
            + "*************************\n\n"
            + "For more information, see"
            + " https://learn.microsoft.com/azure/azure-monitor/app/opentelemetry-enable?tabs=java#modify-your-application\n");
  }

  private Agent() {}
}
