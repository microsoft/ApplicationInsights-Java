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

package com.microsoft.applicationinsights.agent.internal;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.ApplicationInsightsAppenderClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.DependencyTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.HeartBeatModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.PerformanceCounterModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.QuickPulseClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.TelemetryClientClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.WebRequestTrackingFilterClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingPercentage;
import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.agent.bootstrap.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.extensibility.initializer.ResourceAttributesContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.SdkVersionContextInitializer;
import com.microsoft.applicationinsights.internal.channel.common.ApacheSender43;
import com.microsoft.applicationinsights.internal.config.AddTypeXmlElement;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.JmxXmlElement;
import com.microsoft.applicationinsights.internal.config.ParamXmlElement;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.config.TelemetryModulesXmlElement;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import com.microsoft.applicationinsights.web.internal.correlation.CdsProfileFetcher;
import io.opentelemetry.instrumentation.api.aiappid.AiAppId;
import io.opentelemetry.instrumentation.api.aiconnectionstring.AiConnectionString;
import io.opentelemetry.instrumentation.api.config.Config;
import org.apache.http.HttpHost;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BeforeAgentInstaller {

    private static final Logger startupLogger = LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

    private BeforeAgentInstaller() {
    }

    public static void beforeInstallBytebuddyAgent(Instrumentation instrumentation) throws Exception {
        instrumentation.addTransformer(new CommonsLogFactoryClassFileTransformer());
        start(instrumentation);
        // add sdk instrumentation after ensuring Global.getTelemetryClient() will not return null
        instrumentation.addTransformer(new TelemetryClientClassFileTransformer());
        instrumentation.addTransformer(new DependencyTelemetryClassFileTransformer());
        instrumentation.addTransformer(new PerformanceCounterModuleClassFileTransformer());
        instrumentation.addTransformer(new QuickPulseClassFileTransformer());
        instrumentation.addTransformer(new HeartBeatModuleClassFileTransformer());
        instrumentation.addTransformer(new ApplicationInsightsAppenderClassFileTransformer());
        instrumentation.addTransformer(new WebRequestTrackingFilterClassFileTransformer());
    }

    private static void start(Instrumentation instrumentation) throws Exception {

        String codelessSdkNamePrefix = getCodelessSdkNamePrefix();
        if (codelessSdkNamePrefix != null) {
            PropertyHelper.setSdkNamePrefix(codelessSdkNamePrefix);
        }

        File javaTmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpDir = new File(javaTmpDir, "applicationinsights-java");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new Exception("Could not create directory: " + tmpDir.getAbsolutePath());
        }

        Configuration config = MainEntryPoint.getConfiguration();
        if (!hasConnectionStringOrInstrumentationKey(config)) {
            if (!("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME")))) {
                throw new FriendlyException("No connection string or instrumentation key provided",
                                            "Please provide connection string or instrumentation key.");
            }
        }
        // Function to validate user provided processor configuration
        validateProcessorConfiguration(config);


        Map<String, String> properties = new HashMap<>();
        properties.put("additional.bootstrap.package.prefixes", "com.microsoft.applicationinsights.agent.bootstrap");
        properties.put("experimental.log.capture.threshold", getLoggingFrameworksThreshold(config, "INFO"));
        int reportingIntervalSeconds = getMicrometerReportingIntervalSeconds(config, 60);
        properties.put("micrometer.step.millis", Long.toString(SECONDS.toMillis(reportingIntervalSeconds)));
        if (!isInstrumentationEnabled(config, "micrometer")) {
            properties.put("ota.integration.micrometer.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "jdbc")) {
            properties.put("ota.integration.jdbc.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "logging")) {
            properties.put("ota.integration.log4j.enabled", "false");
            properties.put("ota.integration.java-util-logging.enabled", "false");
            properties.put("ota.integration.logback.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "redis")) {
            properties.put("ota.integration.jedis.enabled", "false");
            properties.put("ota.integration.lettuce.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "kafka")) {
            properties.put("ota.integration.kafka.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "mongo")) {
            properties.put("ota.integration.mongo.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "cassandra")) {
            properties.put("ota.integration.cassandra.enabled", "false");
        }
        if (!config.preview.openTelemetryApiSupport) {
            properties.put("ota.integration.opentelemetry-api.enabled", "false");
        }
        Config.internalInitializeConfig(Config.create(properties));
        if (Config.get().getListProperty("additional.bootstrap.package.prefixes").isEmpty()) {
            throw new IllegalStateException("underlying config not initialized in time");
        }

        // FIXME do something with config

        // FIXME set doNotWeavePrefixes = "com.microsoft.applicationinsights.agent."

        // FIXME set tryToLoadInBootstrapClassLoader = "com.microsoft.applicationinsights.agent."
        // (maybe not though, this is only needed for classes in :agent:agent-bootstrap)

        String jbossHome = System.getenv("JBOSS_HOME");
        if (!Strings.isNullOrEmpty(jbossHome)) {
            // this is used to delay SSL initialization because SSL initialization triggers loading of
            // java.util.logging (starting with Java 8u231)
            // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
            ApacheSender43.safeToInitLatch = new CountDownLatch(1);
            instrumentation.addTransformer(new JulListeningClassFileTransformer(ApacheSender43.safeToInitLatch));
        }

        if (config.proxy.host != null) {
            HttpHost proxy = new HttpHost(config.proxy.host, config.proxy.port);
            ApacheSender43.proxy = proxy;
            CdsProfileFetcher.proxy = proxy;
        }

        TelemetryConfiguration configuration = TelemetryConfiguration.getActiveWithoutInitializingConfig();
        TelemetryConfigurationFactory.INSTANCE.initialize(configuration, buildXmlConfiguration(config));
        configuration.getContextInitializers().add(new SdkVersionContextInitializer());
        configuration.getContextInitializers().add(new ResourceAttributesContextInitializer(config.customDimensions));

        Global.setSamplingPercentage(SamplingPercentage.roundToNearest(config.sampling.percentage));
        final TelemetryClient telemetryClient = new TelemetryClient();
        Global.setTelemetryClient(telemetryClient);
        AiAppId.setSupplier(new AppIdSupplier());

        // this is for Azure Function Linux consumption plan support.
        if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
            AiConnectionString.setAccessor(new ConnectionStringAccessor());
        }

        // this is currently used by Micrometer instrumentation in addition to 2.x SDK
        BytecodeUtil.setDelegate(new BytecodeUtilImpl());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                startupLogger.debug("running shutdown hook");
                try {
                    telemetryClient.flush();
                    telemetryClient.shutdown(5, SECONDS);
                    startupLogger.debug("completed shutdown hook");
                } catch (InterruptedException e) {
                    startupLogger.debug("interrupted while flushing telemetry during shutdown");
                } catch (Throwable t) {
                    startupLogger.debug(t.getMessage(), t);
                }
            }
        });

        Path configPath = MainEntryPoint.getConfigPath();
        if (configPath != null) {
            JsonConfigPolling.pollJsonConfigEveryMinute(configPath, MainEntryPoint.getLastModifiedTime(), config.sampling.percentage);
        }
    }

    private static void validateProcessorConfiguration(Configuration config) throws FriendlyException {
        if (config.preview == null || config.preview.processors == null) return;
        for (ProcessorConfig processorConfig : config.preview.processors) {
            processorConfig.validate();
        }
    }

    @Nullable
    private static String getCodelessSdkNamePrefix() {
        StringBuilder sdkNamePrefix = new StringBuilder(4);
        if (DiagnosticsHelper.isAppServiceCodeless()) {
            sdkNamePrefix.append("a");
        } else if (DiagnosticsHelper.isAksCodeless()) {
            sdkNamePrefix.append("k");
        } else if (DiagnosticsHelper.isFunctionsCodeless()) {
            sdkNamePrefix.append("f");
        } else {
            return null;
        }
        if (SystemInformation.INSTANCE.isWindows()) {
            sdkNamePrefix.append("w");
        } else if (SystemInformation.INSTANCE.isUnix()) {
            sdkNamePrefix.append("l");
        } else {
            startupLogger.warn("could not detect os: {}", System.getProperty("os.name"));
            sdkNamePrefix.append("u");
        }
        sdkNamePrefix.append("r_"); // "r" is for "recommended"
        return sdkNamePrefix.toString();
    }

    private static boolean hasConnectionStringOrInstrumentationKey(Configuration config) {
        return !Strings.isNullOrEmpty(config.connectionString)
                || !Strings.isNullOrEmpty(System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"))
                || !Strings.isNullOrEmpty(System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY"));
    }

    private static String getLoggingFrameworksThreshold(Configuration config, String defaultValue) {
        Map<String, Object> logging = config.instrumentation.get("logging");
        if (logging == null) {
            return defaultValue;
        }
        Object levelObj = logging.get("level");
        if (levelObj == null) {
            return defaultValue;
        }
        if (!(levelObj instanceof String)) {
            startupLogger.warn("logging level must be a string, but found: {}", levelObj.getClass());
            return defaultValue;
        }
        String threshold = (String) levelObj;
        if (threshold.isEmpty()) {
            return defaultValue;
        }
        return threshold;
    }

    private static boolean isInstrumentationEnabled(Configuration config, String instrumentationName) {
        Map<String, Object> properties = config.instrumentation.get(instrumentationName);
        if (properties == null) {
            return true;
        }
        Object value = properties.get("enabled");
        if (value == null) {
            return true;
        }
        if (!(value instanceof Boolean)) {
            startupLogger.warn("{} enabled must be a boolean, but found: {}", instrumentationName, value.getClass());
            return true;
        }
        return (Boolean) value;
    }

    private static int getMicrometerReportingIntervalSeconds(Configuration config, int defaultValue) {
        Map<String, Object> micrometer = config.instrumentation.get("micrometer");
        if (micrometer == null) {
            return defaultValue;
        }
        Object value = micrometer.get("reportingIntervalSeconds");
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number)) {
            startupLogger.warn("micrometer reportingIntervalSeconds must be a number, but found: {}", value.getClass());
            return defaultValue;
        }
        return ((Number) value).intValue();
    }

    private static ApplicationInsightsXmlConfiguration buildXmlConfiguration(Configuration config) {

        ApplicationInsightsXmlConfiguration xmlConfiguration = new ApplicationInsightsXmlConfiguration();

        if (!Strings.isNullOrEmpty(config.connectionString)) {
            xmlConfiguration.setConnectionString(config.connectionString);
        }
        if (!Strings.isNullOrEmpty(config.role.name)) {
            xmlConfiguration.setRoleName(config.role.name);
        }
        if (!Strings.isNullOrEmpty(config.role.instance)) {
            xmlConfiguration.setRoleInstance(config.role.instance);
        } else {
            String hostname = CommonUtils.getHostName();
            xmlConfiguration.setRoleInstance(hostname == null ? "unknown" : hostname);
        }

        // configure heartbeat module
        AddTypeXmlElement heartbeatModule = new AddTypeXmlElement();
        heartbeatModule.setType("com.microsoft.applicationinsights.internal.heartbeat.HeartBeatModule");
        // do not allow interval longer than 15 minutes, since we use the heartbeat data for usage telemetry
        long intervalSeconds = Math.min(config.heartbeat.intervalSeconds, MINUTES.toSeconds(15));
        heartbeatModule.getParameters().add(newParamXml("HeartBeatInterval", Long.toString(intervalSeconds)));
        ArrayList<AddTypeXmlElement> modules = new ArrayList<>();
        modules.add(heartbeatModule);
        TelemetryModulesXmlElement modulesXml = new TelemetryModulesXmlElement();
        modulesXml.setAdds(modules);
        xmlConfiguration.setModules(modulesXml);

        // configure custom jmx metrics
        ArrayList<JmxXmlElement> jmxXmls = new ArrayList<>();
        for (JmxMetric jmxMetric : config.jmxMetrics) {
            JmxXmlElement jmxXml = new JmxXmlElement();
            jmxXml.setName(jmxMetric.name);
            jmxXml.setObjectName(jmxMetric.objectName);
            jmxXml.setAttribute(jmxMetric.attribute);
            jmxXmls.add(jmxXml);
        }
        xmlConfiguration.getPerformance().setJmxXmlElements(jmxXmls);

        if (config.preview.developerMode) {
            xmlConfiguration.getChannel().setDeveloperMode(true);
        }
        return xmlConfiguration;
    }

    private static ParamXmlElement newParamXml(String name, String value) {
        ParamXmlElement paramXml = new ParamXmlElement();
        paramXml.setName(name);
        paramXml.setValue(value);
        return paramXml;
    }
}
