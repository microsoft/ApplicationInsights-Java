/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach.override;

import java.io.File;
import java.lang.management.ManagementFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

// Class to replace by an OTel class from the java contrib repo after next java contrib release
public final class RuntimeAttach {

  public static void attachJavaagentToCurrentJVM() {
    if (agentIsDisabled()) {
      return;
    }

    AgentFileProvider agentFileProvider = new AgentFileProvider();
    File agentFile = agentFileProvider.getAgentFile();

    try {
      ByteBuddyAgent.attach(agentFile, getPid());
    } finally {
      agentFileProvider.deleteTempDir();
    }
  }

  private static boolean agentIsDisabled() {
    String enabledProperty =
        System.getProperty("otel.javaagent.enabled", System.getenv("OTEL_JAVAAGENT_ENABLED"));
    return "false".equals(enabledProperty);
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
