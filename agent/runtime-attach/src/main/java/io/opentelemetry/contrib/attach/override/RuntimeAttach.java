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
