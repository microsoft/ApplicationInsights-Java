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

package com.microsoft.applicationinsights.agentc.internal;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agentc.internal.Configuration.FixedRateSampling;
import com.microsoft.applicationinsights.agentc.internal.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.status.StatusFile;
import com.microsoft.applicationinsights.agentc.internal.model.Global;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.JmxXmlElement;
import com.microsoft.applicationinsights.internal.config.SDKLoggerXmlElement;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptors;
import org.glowroot.instrumentation.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.instrumentation.engine.impl.SimpleConfigServiceFactory;
import org.glowroot.instrumentation.engine.init.EngineModule;
import org.glowroot.instrumentation.engine.init.MainEntryPointUtil;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class MainEntryPoint {

    private static @Nullable Logger startupLogger;

    private MainEntryPoint() {
    }

    public static void premain(Instrumentation instrumentation, File agentJarFile) {
        try {
            DiagnosticsHelper.setAgentJarFile(agentJarFile);
            startupLogger = initLogging(instrumentation, agentJarFile);
            MDC.put("microsoft.ai.operationName", "Startup");
            addLibJars(instrumentation, agentJarFile);
            instrumentation.addTransformer(new CommonsLogFactoryClassFileTransformer());
            start(instrumentation, agentJarFile);
            // add legacy class file transformers after ensuring Global.getTelemetryClient() will not return null
            instrumentation.addTransformer(new LegacyTelemetryClientClassFileTransformer());
            instrumentation.addTransformer(new LegacyDependencyTelemetryClassFileTransformer());
            instrumentation.addTransformer(new LegacyPerformanceCounterClassFileTransformer());
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            startupLogger.error("Agent failed to start.", t);
            t.printStackTrace();
        } finally {
            try {
                StatusFile.write();
            } catch (Exception e) {
                startupLogger.error("Error writing status.json", e);
            }
            MDC.clear();
        }
    }

    public static Logger initLogging(Instrumentation instrumentation, File agentJarFile) {
        File logbackXmlOverride = new File(agentJarFile.getParentFile(), "ai.logback.xml");
        if (logbackXmlOverride.exists()) {
            System.setProperty("ai.logback.configurationFile", logbackXmlOverride.getAbsolutePath());
        }
        try {
            return MainEntryPointUtil.initLogging("com.microsoft.applicationinsights", instrumentation);
        } finally {
            System.clearProperty("ai.logback.configurationFile");
        }
    }

    private static void addLibJars(Instrumentation instrumentation, File agentJarFile) throws Exception {
        File libDir = new File(agentJarFile.getParentFile(), "lib");
        if (!libDir.exists()) {
            return;
        }
        File[] files = libDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(file));
            }
        }
    }

    private static void start(Instrumentation instrumentation, File agentJarFile) throws Exception {

        if (DiagnosticsHelper.isAppService()) {
            if (SystemInformation.INSTANCE.isWindows()) {
                PropertyHelper.setSdkNamePrefix("awr_");
            } else if (SystemInformation.INSTANCE.isUnix()) {
                PropertyHelper.setSdkNamePrefix("alr_");
            } else {
                startupLogger.warn("could not detect os: {}", System.getProperty("os.name"));
                PropertyHelper.setSdkNamePrefix("aur_");
            }
        }

        File javaTmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpDir = new File(javaTmpDir, "applicationinsights-java");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new Exception("Could not create directory: " + tmpDir.getAbsolutePath());
        }

        Configuration config = ConfigurationBuilder.create(agentJarFile.toPath());

        Global.setDistributedTracingOutboundEnabled(config.distributedTracing.outboundEnabled);
        Global.setDistributedTracingRequestIdCompatEnabled(config.distributedTracing.requestIdCompatEnabled);

        ApplicationInsightsXmlConfiguration xmlConfiguration = new ApplicationInsightsXmlConfiguration();

        if (!Strings.isNullOrEmpty(config.connectionString)) {
            xmlConfiguration.setConnectionString(config.connectionString);
        }
        if (!Strings.isNullOrEmpty(config.roleName)) {
            xmlConfiguration.setRoleName(config.roleName);
        }
        if (!Strings.isNullOrEmpty(config.roleInstance)) {
            xmlConfiguration.setRoleInstance(config.roleInstance);
        }
        if (!config.liveMetrics.enabled) {
            xmlConfiguration.getQuickPulse().setEnabled(false);
        }
        ArrayList<JmxXmlElement> jmxXmls = new ArrayList<>();
        for (JmxMetric jmxMetric : config.jmxMetrics) {
            JmxXmlElement jmxXml = new JmxXmlElement();
            jmxXml.setObjectName(jmxMetric.objectName);
            jmxXml.setAttribute(jmxMetric.attribute);
            jmxXml.setDisplayName(jmxMetric.display);
            if (jmxMetric.attribute.indexOf('.') != -1) {
                jmxXml.setType("COMPOSITE");
            }
            jmxXmls.add(jmxXml);
        }
        xmlConfiguration.getPerformance().setJmxXmlElements(jmxXmls);

        if (config.debug) {
            SDKLoggerXmlElement sdkLogger = new SDKLoggerXmlElement();
            sdkLogger.setType("CONSOLE");
            sdkLogger.setLevel("TRACE");
            xmlConfiguration.setSdkLogger(sdkLogger);
        }
        if (config.developerMode) {
            xmlConfiguration.getChannel().setDeveloperMode(true);
        }

        List<InstrumentationDescriptor> instrumentationDescriptors = InstrumentationDescriptors.read();
        InstrumentationDescriptor customInstrumentationDescriptor =
                CustomInstrumentationBuilder.buildCustomInstrumentation(config);
        if (customInstrumentationDescriptor != null) {
            instrumentationDescriptors = new ArrayList<>(instrumentationDescriptors);
            instrumentationDescriptors.add(customInstrumentationDescriptor);
        }

        ConfigServiceFactory configServiceFactory =
                new SimpleConfigServiceFactory(instrumentationDescriptors, getInstrumentationConfig(config));

        // need to add instrumentation transformers before initializing telemetry configuration below, since that starts
        // some threads and loads some classes that the instrumentation wants to weave
        EngineModule.createWithSomeDefaults(instrumentation, tmpDir, Global.getThreadContextThreadLocal(),
                instrumentationDescriptors, configServiceFactory, new AgentImpl(), false,
                Collections.singletonList("com.microsoft.applicationinsights.agentc."),
                Collections.singletonList("com.microsoft.applicationinsights.agentc."), agentJarFile);

        TelemetryConfiguration configuration = TelemetryConfiguration.getActiveWithoutInitializingConfig();
        TelemetryConfigurationFactory.INSTANCE.initialize(configuration, xmlConfiguration);
        FixedRateSampling fixedRateSampling = config.fixedRateSampling;
        if (fixedRateSampling != null) {
            addFixedRateSampling(fixedRateSampling, configuration);
        }
        TelemetryClient telemetryClient = new TelemetryClient();
        if (!config.telemetryContext.isEmpty()) {
            telemetryClient.getContext().getProperties().putAll(config.telemetryContext);
        }
        Global.setTelemetryClient(telemetryClient);
    }

    private static Map<String, Map<String, Object>> getInstrumentationConfig(Configuration configuration) {

        Map<String, Map<String, Object>> instrumentationConfig = new HashMap<>();

        Map<String, Object> servletConfig = new HashMap<>();
        servletConfig.put("captureRequestServerHostname", true);
        servletConfig.put("captureRequestServerPort", true);
        servletConfig.put("captureRequestScheme", true);
        servletConfig.put("captureRequestCookies", Arrays.asList("ai_user", "ai_session"));

        Map<String, Object> jdbcConfig = new HashMap<>();
        jdbcConfig.put("captureBindParametersIncludes", Collections.emptyList());
        jdbcConfig.put("captureResultSetNavigate", false);
        jdbcConfig.put("captureGetConnection", false);

        Number explainPlanThresholdInMS = getExplainPlanThresholdInMS(configuration);
        if (explainPlanThresholdInMS == null) {
            jdbcConfig.put("explainPlanThresholdMillis", 10000);
        } else {
            jdbcConfig.put("explainPlanThresholdMillis", explainPlanThresholdInMS);
        }

        Map<String, Object> log4jConfig = new HashMap<>();
        Map<String, Object> logbackConfig = new HashMap<>();
        Map<String, Object> julConfig = new HashMap<>();

        LoggingThreshold threshold = getLoggingThreshold(configuration);
        if (threshold == null) {
            log4jConfig.put("threshold", "warn");
            logbackConfig.put("threshold", "warn");
            julConfig.put("threshold", "warning");
        } else {
            log4jConfig.put("threshold", threshold.log4jThreshold);
            logbackConfig.put("threshold", threshold.logbackThreshold);
            julConfig.put("threshold", threshold.julThreshold);
        }
        instrumentationConfig.put("servlet", servletConfig);
        instrumentationConfig.put("jdbc", jdbcConfig);
        instrumentationConfig.put("log4j", log4jConfig);
        instrumentationConfig.put("logback", logbackConfig);
        instrumentationConfig.put("java-util-logging", julConfig);

        return instrumentationConfig;
    }

    private static @Nullable Number getExplainPlanThresholdInMS(Configuration configuration) {
        Map<String, Object> jdbc = configuration.instrumentation.get("jdbc");
        if (jdbc == null) {
            return null;
        }
        Object explainPlanThresholdInMS = jdbc.get("explainPlanThresholdInMS");
        if (explainPlanThresholdInMS == null) {
            return null;
        }
        if (!(explainPlanThresholdInMS instanceof Number)) {
            startupLogger.warn("jdbc explainPlanThresholdMillis must be a number, but found: {}",
                    explainPlanThresholdInMS.getClass());
            return null;
        }
        return (Number) explainPlanThresholdInMS;
    }

    private static @Nullable LoggingThreshold getLoggingThreshold(Configuration configuration) {
        Map<String, Object> logging = configuration.instrumentation.get("logging");
        if (logging == null) {
            return null;
        }
        Object thresholdObj = logging.get("threshold");
        if (thresholdObj == null) {
            return null;
        }
        if (!(thresholdObj instanceof String)) {
            startupLogger.warn("logging threshold must be a string, but found: {}", thresholdObj.getClass());
            return null;
        }
        String threshold = (String) thresholdObj;
        if (threshold.isEmpty()) {
            return null;
        }
        switch (threshold) {
            case "fatal":
                return new LoggingThreshold("fatal", "error", "severe");
            case "error":
            case "severe":
                return new LoggingThreshold("error", "error", "severe");
            case "warn":
            case "warning":
                return new LoggingThreshold("warn", "warn", "warning");
            case "info":
                return new LoggingThreshold("info", "info", "info");
            case "debug":
            case "config":
            case "fine":
            case "finer":
                return new LoggingThreshold("debug", "debug", threshold);
            case "trace":
            case "finest":
                return new LoggingThreshold("trace", "trace", "finest");
            default:
                startupLogger.warn("invalid logging threshold: {}", threshold);
                return null;
        }
    }

    private static void addFixedRateSampling(FixedRateSampling fixedRateSampling,
                                             TelemetryConfiguration configuration) {

        Map<String, Class<?>> mapping = new HashMap<>();
        mapping.put("Dependency", RemoteDependencyTelemetry.class);
        mapping.put("Event", EventTelemetry.class);
        mapping.put("Exception", ExceptionTelemetry.class);
        mapping.put("PageView", PageViewTelemetry.class);
        mapping.put("Request", RequestTelemetry.class);
        mapping.put("Trace", TraceTelemetry.class);

        double samplingPercentage = MoreObjects.firstNonNull(fixedRateSampling.samplingPercentage, 100.0);
        Map<Class<?>, Double> samplingPercentages = new HashMap<>();

        for (Map.Entry<String, Double> entry : fixedRateSampling.samplingPercentages.entrySet()) {
            String telemetryType = entry.getKey();
            Class clazz = mapping.get(telemetryType);
            if (clazz == null) {
                startupLogger.warn("FixedRateSampling configuration does not support telemetry type: {}",
                        telemetryType);
            } else {
                samplingPercentages.put(clazz, entry.getValue());
            }
        }
        configuration.getTelemetryProcessors()
                .add(new FixedRateSamplingTelemetryProcessor(samplingPercentage, samplingPercentages));
    }

    private static class Connection {
        // this is never an empty string (empty string is normalized to null)
        private @Nullable String instrumentationKey;
        private String ingestionEndpoint = "https://dc.services.visualstudio.com/";
    }

    private static class LoggingThreshold {

        private static final LoggingThreshold DEFAULT = new LoggingThreshold("warn", "warn", "warning");

        private final String log4jThreshold;
        private final String logbackThreshold;
        private final String julThreshold;

        private LoggingThreshold(String log4jThreshold, String logbackThreshold, String julThreshold) {
            this.log4jThreshold = log4jThreshold;
            this.logbackThreshold = logbackThreshold;
            this.julThreshold = julThreshold;
        }
    }
}
