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

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.PidFinder;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status.StatusFile;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SelfDiagnostics;
import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfigurationBuilder;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import io.opentelemetry.javaagent.bootstrap.JavaagentFileHolder;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// this class initializes configuration and logging before passing control to
// opentelemetry-java-instrumentation
@AutoService(LoggingCustomizer.class)
public class MainEntryPoint implements LoggingCustomizer {

  private static final boolean DEBUG_SIGNED_JAR_ACCESS =
      Boolean.getBoolean("applicationinsights.debug.signedJarAccess");

  private static final File javaagentFile = JavaagentFileHolder.getJavaagentFile();

  private static RpConfiguration rpConfiguration;
  private static Configuration configuration;
  private static String agentVersion = "(unknown)";

  @Nullable private static volatile Logger startupLogger;

  public static RpConfiguration getRpConfiguration() {
    return rpConfiguration;
  }

  public static Configuration getConfiguration() {
    return configuration;
  }

  public static String getAgentVersion() {
    return agentVersion;
  }

  @Override
  public void init() {
    try {
      if (DEBUG_SIGNED_JAR_ACCESS) {
        JarVerifierClassFileTransformer transformer = new JarVerifierClassFileTransformer();
        Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
        instrumentation.addTransformer(transformer, true);
        instrumentation.retransformClasses(Class.forName("java.util.jar.JarVerifier"));
        instrumentation.removeTransformer(transformer);
      }
      Path agentPath = javaagentFile.toPath();
      // need to initialize version before initializing DiagnosticsHelper
      agentVersion = SdkVersionFinder.initVersion(agentPath);
      DiagnosticsHelper.setAgentJarFile(agentPath);
      // configuration is only read this early in order to extract logging configuration
      rpConfiguration = RpConfigurationBuilder.create(agentPath);
      configuration = ConfigurationBuilder.create(agentPath, rpConfiguration);

      String codelessSdkNamePrefix = getCodelessSdkNamePrefix();
      if (codelessSdkNamePrefix != null) {
        PropertyHelper.setSdkNamePrefix(codelessSdkNamePrefix);
      }
      startupLogger = configureLogging(configuration.selfDiagnostics, agentPath);
      StatusFile.startupLogger = startupLogger;
      ConfigurationBuilder.logConfigurationWarnMessages();

      AppIdSupplier appIdSupplier = AiComponentInstaller.beforeAgent();
      StartAppIdRetrieval.setAppIdSupplier(appIdSupplier);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void onStartupSuccess() {
    startupLogger.info(
        "ApplicationInsights Java Agent {} started successfully (PID {})",
        agentVersion,
        new PidFinder().getValue());
    startupLogger.info(
        "Java version: {}, vendor: {}, home: {}",
        System.getProperty("java.version"),
        System.getProperty("java.vendor"),
        System.getProperty("java.home"));

    MDC.put(DiagnosticsHelper.MDC_PROP_OPERATION, "Startup");
    try {
      LoggerFactory.getLogger(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME)
          .info("Application Insights Codeless Agent {} Attach Successful", agentVersion);
    } finally {
      MDC.clear();
    }

    updateStatusFile(true);
  }

  @Override
  public void onStartupFailure(Throwable throwable) {
    FriendlyException friendlyException = getFriendlyException(throwable);
    String banner =
        "ApplicationInsights Java Agent "
            + agentVersion
            + " failed to start (PID "
            + new PidFinder().getValue()
            + ")";

    if (friendlyException != null) {
      logErrorMessage(
          startupLogger,
          friendlyException.getMessageWithBanner(banner),
          true,
          throwable,
          javaagentFile);
    } else {
      logErrorMessage(startupLogger, banner, false, throwable, javaagentFile);
    }

    updateStatusFile(false);
  }

  private static void updateStatusFile(boolean success) {
    try {
      StatusFile.putValueAndWrite("AgentInitializedSuccessfully", success, startupLogger != null);
    } catch (Throwable t) {
      if (startupLogger != null) {
        startupLogger.error("Error writing status.json", t);
      } else {
        t.printStackTrace();
      }
    }
  }

  // visible for testing
  @Nullable
  static FriendlyException getFriendlyException(Throwable t) {
    if (t instanceof FriendlyException) {
      return (FriendlyException) t;
    }
    Throwable cause = t.getCause();
    if (cause == null) {
      return null;
    }
    return getFriendlyException(cause);
  }

  @SuppressWarnings("SystemOut")
  private static void logErrorMessage(
      @Nullable Logger startupLogger,
      String message,
      boolean isFriendlyException,
      Throwable t,
      File javaagentFile) {

    if (startupLogger != null) {
      if (isFriendlyException) {
        startupLogger.error(message);
      } else {
        startupLogger.error(message, t);
      }
    } else {
      try {
        // IF the startupLogger failed to be initialized due to configuration syntax error, try
        // initializing it here
        Path agentPath = javaagentFile.toPath();
        SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
        selfDiagnostics.file.path =
            ConfigurationBuilder.overlayWithEnvVar(
                ConfigurationBuilder.APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH,
                selfDiagnostics.file.path);
        startupLogger = configureLogging(selfDiagnostics, agentPath);
        if (isFriendlyException) {
          startupLogger.error(message);
        } else {
          startupLogger.error(message, t);
        }
      } catch (Throwable ignored) {
        // this is a last resort in cases where the JVM doesn't have write permission to the
        // directory where the agent lives
        // and the user has not specified APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH

        // If the startupLogger still have some issues being initialized, print the error stack
        // trace to stderr
        if (isFriendlyException) {
          System.err.println(message);
        } else {
          t.printStackTrace();
        }
        // and write to a temp file because some environments do not have (easy) access to stderr
        String tmpDir = System.getProperty("java.io.tmpdir");
        File file = new File(tmpDir, "applicationinsights.log");
        try {
          Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
          out.write(message);
          out.close();
        } catch (Throwable ignored2) {
          // ignored
        }
      }
    }
  }

  private static Logger configureLogging(SelfDiagnostics selfDiagnostics, Path agentPath) {
    new LoggingConfigurator(selfDiagnostics, agentPath).configure();
    return LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");
  }

  @Nullable
  private static String getCodelessSdkNamePrefix() {
    if (!DiagnosticsHelper.isRpIntegration()) {
      return null;
    }
    StringBuilder sdkNamePrefix = new StringBuilder(4);
    sdkNamePrefix.append(DiagnosticsHelper.rpIntegrationChar());
    if (SystemInformation.isWindows()) {
      sdkNamePrefix.append("w");
    } else if (SystemInformation.isLinux()) {
      sdkNamePrefix.append("l");
    } else {
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent")
          .warn("could not detect os: {}", System.getProperty("os.name"));
      sdkNamePrefix.append("u");
    }
    sdkNamePrefix.append("r_"); // "r" is for "recommended"
    return sdkNamePrefix.toString();
  }
}
