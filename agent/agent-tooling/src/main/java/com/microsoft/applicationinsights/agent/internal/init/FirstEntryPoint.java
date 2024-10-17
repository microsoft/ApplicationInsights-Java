// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.MsgId.INITIALIZATION_SUCCESS;
import static com.microsoft.applicationinsights.agent.internal.diagnostics.MsgId.STARTUP_FAILURE_ERROR;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.PropertyHelper;
import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SelfDiagnostics;
import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.diagnostics.PidFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.status.StatusFile;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import io.opentelemetry.javaagent.bootstrap.JavaagentFileHolder;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// this class initializes configuration and logging before passing control to
// opentelemetry-java-instrumentation
@AutoService(LoggingCustomizer.class)
public class FirstEntryPoint implements LoggingCustomizer {

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
  public String name() {
    return "applicationinsights";
  }

  @Override
  public void init(EarlyInitAgentConfig earlyConfig) {
    try {
      if (DEBUG_SIGNED_JAR_ACCESS) {
        JarVerifierClassFileTransformer transformer = new JarVerifierClassFileTransformer();
        Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
        if (instrumentation != null) {
          instrumentation.addTransformer(transformer, true);
          instrumentation.retransformClasses(Class.forName("java.util.jar.JarVerifier"));
          instrumentation.removeTransformer(transformer);
        }
      }
      Path agentPath = javaagentFile.toPath();
      // need to initialize version before initializing DiagnosticsHelper
      agentVersion = SdkVersionFinder.initVersion(agentPath);
      DiagnosticsHelper.initRpIntegration(agentPath);
      // configuration is only read this early in order to extract logging configuration
      rpConfiguration = RpConfigurationBuilder.create(agentPath);
      configuration =
          ConfigurationBuilder.create(
              agentPath, rpConfiguration, System::getenv, System::getProperty);

      String codelessSdkNamePrefix = getCodelessSdkNamePrefix();
      if (codelessSdkNamePrefix != null) {
        PropertyHelper.setSdkNamePrefix(codelessSdkNamePrefix);
      }
      startupLogger = configureLogging(configuration.selfDiagnostics, agentPath);
      ConfigurationBuilder.logConfigurationWarnMessages();

      ClassicSdkInstrumentation.registerTransformers();

      StartupDiagnostics.execute();

      InternalLogger.initialize(Slf4jInternalLogger::create);

      if (JvmCompiler.hasToDisableJvmCompilerDirectives()) {
        JvmCompiler.disableJvmCompilerDirectives();
      }

      checkTlsConnectionsToVirtualServersEnabled();
      if (startupLogger.isDebugEnabled()) {
        startupLogger.debug(
            "Input arguments: {} ", ManagementFactory.getRuntimeMXBean().getInputArguments());
        startupLogger.debug("_JAVA_OPTIONS: {}", System.getenv("_JAVA_OPTIONS"));
        startupLogger.debug("JAVA_TOOL_OPTIONS: {}", System.getenv("JAVA_TOOL_OPTIONS"));
      }

      if (startupLogger.isTraceEnabled()) {
        startupLogger.trace("OS: {}", System.getProperty("os.name"));
        startupLogger.trace("Classpath: {}", System.getProperty("java.class.path"));
        startupLogger.trace("Netty versions: {} ", NettyVersions.extract());
        startupLogger.trace("Env: {}", System.getenv());
        startupLogger.trace("System properties: {} ", findSystemProperties());
      }

      if (startupLogger.isTraceEnabled()) {
        AppInsightsCertificate appInsightsCertificate = new AppInsightsCertificate(startupLogger);
        startupLogger.trace(
            "Application Insights root certificate in the Java keystore: {}",
            appInsightsCertificate.isInJavaKeystore());
      }

    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static void checkTlsConnectionsToVirtualServersEnabled() {
    String tlsConnectionsToVirtualServersProp = "jsse.enableSNIExtension";
    String propValue = System.getProperty(tlsConnectionsToVirtualServersProp);
    if ("false".equals(propValue)) {
      startupLogger.warn(
          "System property -Djsse.enableSNIExtension=false is detected. If you have connection issues with Application Insights, please remove this.");
    }
  }

  private static String findSystemProperties() {
    Properties properties = System.getProperties();
    StringBuilder propsBuilder = new StringBuilder();
    properties.forEach(
        (key, value) -> {
          boolean firstProperty = propsBuilder.length() == 0;
          if (!firstProperty) {
            propsBuilder.append(", ");
          }
          propsBuilder.append("(" + key + "=" + value + ")");
        });
    return propsBuilder.toString();
  }

  @Override
  @SuppressFBWarnings(
      value = "SECCRLFLOG", // CRLF injection into log messages
      justification =
          "Logging params cannot be controlled by an end user of the instrumented application")
  public void onStartupSuccess() {
    startupLogger.info(
        "Application Insights Java Agent {} started successfully (PID {}, JVM running for {} s)",
        agentVersion,
        new PidFinder().getValue(System::getenv),
        findJvmUptimeInSeconds());

    String javaVersion = System.getProperty("java.version");
    String javaVendor = System.getProperty("java.vendor");
    String javaHome = System.getProperty("java.home");
    startupLogger.info("Java version: {}, vendor: {}, home: {}", javaVersion, javaVendor, javaHome);

    MDC.put(DiagnosticsHelper.MDC_PROP_OPERATION, "Startup");
    try (MDC.MDCCloseable ignored = INITIALIZATION_SUCCESS.makeActive()) {
      LoggerFactory.getLogger(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME)
          .info(
              "Application Insights Java Agent {} started successfully; Java version: {}, vendor: {}",
              agentVersion,
              javaVersion,
              javaVendor);
    } finally {
      MDC.remove(DiagnosticsHelper.MDC_PROP_OPERATION);
    }

    updateStatusFile(true);
  }

  private static double findJvmUptimeInSeconds() {
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    return runtimeMxBean.getUptime() / 1000.0;
  }

  @Override
  public void onStartupFailure(Throwable throwable) {
    FriendlyException friendlyException = getFriendlyException(throwable);
    String banner =
        "Application Insights Java Agent "
            + agentVersion
            + " startup failed (PID "
            + new PidFinder().getValue(System::getenv)
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
    StatusFile.putValueAndWrite("AgentInitializedSuccessfully", success, startupLogger != null);
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
      logStartupFailure(isFriendlyException, message, t);
    } else {
      try {
        // IF the startupLogger failed to be initialized due to configuration syntax error, try
        // initializing it here
        Path agentPath = javaagentFile.toPath();
        SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
        selfDiagnostics.file.path =
            ConfigurationBuilder.overlayWithEnvVar(
                ConfigurationBuilder.APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH,
                selfDiagnostics.file.path,
                System::getenv);
        startupLogger = configureLogging(selfDiagnostics, agentPath);

        logStartupFailure(isFriendlyException, message, t);
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

  @SuppressFBWarnings(
      value = "SECCRLFLOG", // CRLF injection into log messages
      justification =
          "Startup failure message cannot be controlled by an end user of the instrumented application")
  private static void logStartupFailure(boolean isFriendlyException, String message, Throwable t) {
    try (MDC.MDCCloseable ignored = STARTUP_FAILURE_ERROR.makeActive()) {
      if (isFriendlyException) {
        startupLogger.error(message);
      } else {
        startupLogger.error(message, t);
      }
    }
  }

  private static Logger configureLogging(SelfDiagnostics selfDiagnostics, Path agentPath) {
    new LoggingConfigurator(selfDiagnostics, agentPath).configure();
    return LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");
  }

  @Nullable
  private static String getCodelessSdkNamePrefix() {
    if (isRuntimeAttached()) {
      return "ra_";
    }
    if (!PropertyHelper.isRpIntegration()) {
      return null;
    }
    return PropertyHelper.getRpIntegrationSdkNamePrefix();
  }

  private static boolean isRuntimeAttached() {
    return Boolean.getBoolean("applicationinsights.internal.runtime.attached");
  }
}
