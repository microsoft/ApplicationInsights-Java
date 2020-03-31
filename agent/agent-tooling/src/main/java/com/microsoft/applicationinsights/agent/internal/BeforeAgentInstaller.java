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
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.ConfigurationBuilder.ConfigurationException;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.FixedRateSampling;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.JmxMetric;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.DependencyTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.HeartBeatModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.PerformanceCounterModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.QuickPulseClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.instrumentation.sdk.TelemetryClientClassFileTransformer;
import com.microsoft.applicationinsights.extensibility.initializer.CloudInfoContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.DeviceInfoContextInitializer;
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
import io.opentelemetry.auto.bootstrap.instrumentation.aiappid.AiAppId;
import io.opentelemetry.auto.config.Config;
import io.opentelemetry.auto.config.ConfigOverride;
import org.apache.http.HttpHost;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeAgentInstaller {

    private static Logger startupLogger = LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

    private BeforeAgentInstaller() {
    }

    public static void beforeInstallBytebuddyAgent(Instrumentation instrumentation, URL bootstrapURL) throws Exception {
        File agentJarFile = new File(bootstrapURL.toURI());
        instrumentation.addTransformer(new CommonsLogFactoryClassFileTransformer());
        start(instrumentation, agentJarFile);
        // add sdk instrumentation after ensuring Global.getTelemetryClient() will not return null
        instrumentation.addTransformer(new TelemetryClientClassFileTransformer());
        instrumentation.addTransformer(new DependencyTelemetryClassFileTransformer());
        instrumentation.addTransformer(new PerformanceCounterModuleClassFileTransformer());
        instrumentation.addTransformer(new QuickPulseClassFileTransformer());
        instrumentation.addTransformer(new HeartBeatModuleClassFileTransformer());
    }

    private static void start(Instrumentation instrumentation, File agentJarFile) throws Exception {

        String codelessSdkNamePrefix = getCodelessSdkNamePrefix();
        if (codelessSdkNamePrefix != null) {
            PropertyHelper.setSdkNamePrefix(codelessSdkNamePrefix);
        }

        File javaTmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpDir = new File(javaTmpDir, "applicationinsights-java");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new Exception("Could not create directory: " + tmpDir.getAbsolutePath());
        }

        InstrumentationSettings config = MainEntryPoint.getConfiguration();
        if (!hasConnectionStringOrInstrumentationKey(config)) {
            throw new ConfigurationException("No connection string or instrumentation key provided");
        }

        Properties properties = new Properties();
        properties.put("additional.bootstrap.package.prefixes", "com.microsoft.applicationinsights.agent.bootstrap");
        properties.put("experimental.log.capture.threshold", getThreshold(config, "WARN"));
        properties.put("experimental.controller-and-view.spans.enabled", "false");
        properties.put("http.server.error.statuses", "400-599");
        ConfigOverride.set(properties);
        if (Config.get().getAdditionalBootstrapPackagePrefixes().isEmpty()) {
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

        if (config.preview.httpProxy.host != null) {
            ApacheSender43.proxy = new HttpHost(config.preview.httpProxy.host, config.preview.httpProxy.port);
        }

        TelemetryConfiguration configuration = TelemetryConfiguration.getActiveWithoutInitializingConfig();
        TelemetryConfigurationFactory.INSTANCE.initialize(configuration, buildXmlConfiguration(config));
        configuration.getContextInitializers().add(new SdkVersionContextInitializer());
        configuration.getContextInitializers().add(new DeviceInfoContextInitializer());
        configuration.getContextInitializers().add(new CloudInfoContextInitializer());

        FixedRateSampling fixedRateSampling = config.preview.sampling.fixedRate;
        if (fixedRateSampling != null && fixedRateSampling.percentage != null) {
            Global.setFixedRateSamplingPercentage(fixedRateSampling.percentage);
        }
        TelemetryClient telemetryClient = new TelemetryClient();
        Global.setTelemetryClient(telemetryClient);
        AiAppId.setSupplier(new AppIdSupplier());
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

    private static boolean hasConnectionStringOrInstrumentationKey(InstrumentationSettings config) {
        return !Strings.isNullOrEmpty(config.connectionString)
                || !Strings.isNullOrEmpty(System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"))
                || !Strings.isNullOrEmpty(System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY"));
    }

    private static String getThreshold(InstrumentationSettings config, String defaultValue) {
        Map<String, Object> logging = config.preview.instrumentation.get("logging");
        if (logging == null) {
            return defaultValue;
        }
        Object thresholdObj = logging.get("threshold");
        if (thresholdObj == null) {
            return defaultValue;
        }
        if (!(thresholdObj instanceof String)) {
            startupLogger.warn("logging threshold must be a string, but found: {}", thresholdObj.getClass());
            return defaultValue;
        }
        String threshold = (String) thresholdObj;
        if (threshold.isEmpty()) {
            return defaultValue;
        }
        return threshold;
    }

    private static ApplicationInsightsXmlConfiguration buildXmlConfiguration(InstrumentationSettings config) {

        ApplicationInsightsXmlConfiguration xmlConfiguration = new ApplicationInsightsXmlConfiguration();

        if (!Strings.isNullOrEmpty(config.connectionString)) {
            xmlConfiguration.setConnectionString(config.connectionString);
        }
        if (!Strings.isNullOrEmpty(config.preview.roleName)) {
            xmlConfiguration.setRoleName(config.preview.roleName);
        }
        if (!Strings.isNullOrEmpty(config.preview.roleInstance)) {
            xmlConfiguration.setRoleInstance(config.preview.roleInstance);
        }
        if (!config.preview.liveMetrics.enabled) {
            xmlConfiguration.getQuickPulse().setEnabled(false);
        }

        // configure heartbeat module
        AddTypeXmlElement heartbeatModule = new AddTypeXmlElement();
        heartbeatModule.setType("com.microsoft.applicationinsights.internal.heartbeat.HeartBeatModule");
        heartbeatModule.getParameters().add(newParamXml("isHeartBeatEnabled",
                Boolean.toString(config.preview.heartbeat.enabled)));
        heartbeatModule.getParameters().add(newParamXml("HeartBeatInterval",
                Long.toString(config.preview.heartbeat.intervalSeconds)));
        ArrayList<AddTypeXmlElement> modules = new ArrayList<>();
        modules.add(heartbeatModule);
        TelemetryModulesXmlElement modulesXml = new TelemetryModulesXmlElement();
        modulesXml.setAdds(modules);
        xmlConfiguration.setModules(modulesXml);

        // configure custom jmx metrics
        ArrayList<JmxXmlElement> jmxXmls = new ArrayList<>();
        for (JmxMetric jmxMetric : config.preview.jmxMetrics) {
            JmxXmlElement jmxXml = new JmxXmlElement();
            jmxXml.setObjectName(jmxMetric.objectName);
            jmxXml.setAttribute(jmxMetric.attribute);
            jmxXml.setDisplayName(jmxMetric.display);
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
