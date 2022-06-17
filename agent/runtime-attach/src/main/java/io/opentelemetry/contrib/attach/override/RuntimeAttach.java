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
import java.util.logging.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;

// Class to replace by an OTel class from the java contrib repo after next java contrib release
public final class RuntimeAttach {

  private static final Logger logger =
      Logger.getLogger(io.opentelemetry.contrib.attach.RuntimeAttach.class.getName());
  private static final String AGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String AGENT_ENABLED_ENV_VAR = "OTEL_JAVAAGENT_ENABLED";
  static final String MAIN_METHOD_CHECK_PROP =
      "otel.javaagent.testing.runtime-attach.main-method-check";

  public static void attachJavaagentToCurrentJvm() {
    if (!shouldAttach()) {
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

  private static boolean shouldAttach() {
    if (agentIsDisabledWithProp()) {
      logger.fine("Agent was disabled with " + AGENT_ENABLED_PROPERTY + " property.");
      return false;
    }
    if (agentIsDisabledWithEnvVar()) {
      logger.fine("Agent was disabled with " + AGENT_ENABLED_ENV_VAR + " environment variable.");
      return false;
    }
    if (agentIsAttached()) {
      logger.fine("Agent is already attached. It is not attached a second time.");
      return false;
    }
    if (mainMethodCheckIsEnabled() && !isMainThread()) {
      logger.warning(
          "Agent is not attached because runtime attachment was not requested from main thread.");
      return false;
    }
    if (mainMethodCheckIsEnabled() && !isMainMethod()) {
      logger.warning(
          "Agent is not attached because runtime attachment was not requested from main method.");
      return false;
    }
    return true;
  }

  private static boolean agentIsDisabledWithProp() {
    String agentEnabledPropValue = System.getProperty(AGENT_ENABLED_PROPERTY);
    return "false".equalsIgnoreCase(agentEnabledPropValue);
  }

  private static boolean agentIsDisabledWithEnvVar() {
    String agentEnabledEnvVarValue = System.getenv(AGENT_ENABLED_ENV_VAR);
    return "false".equals(agentEnabledEnvVarValue);
  }

  private static boolean agentIsAttached() {
    try {
      Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean mainMethodCheckIsEnabled() {
    String mainThreadCheck = System.getProperty(MAIN_METHOD_CHECK_PROP);
    return !"false".equals(mainThreadCheck);
  }

  private static boolean isMainThread() {
    Thread currentThread = Thread.currentThread();
    return "main".equals(currentThread.getName());
  }

  static boolean isMainMethod() {
    StackTraceElement bottomOfStack = findBottomOfStack(Thread.currentThread());
    String methodName = bottomOfStack.getMethodName();
    return "main".equals(methodName);
  }

  private static StackTraceElement findBottomOfStack(Thread thread) {
    StackTraceElement[] stackTrace = thread.getStackTrace();
    return stackTrace[stackTrace.length - 1];
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
