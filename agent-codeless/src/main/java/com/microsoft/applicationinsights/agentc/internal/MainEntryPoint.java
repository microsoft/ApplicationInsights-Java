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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
import com.microsoft.applicationinsights.internal.channel.common.ApacheSender43;
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

    @Nullable
    private static Logger startupLogger;

    private MainEntryPoint() {
    }

    public static void premain(Instrumentation instrumentation, File agentJarFile) {
        boolean success = false;
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
            success = true;
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

        if (DiagnosticsHelper.isAppServiceCodeless()) {
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

        List<InstrumentationDescriptor> instrumentationDescriptors = InstrumentationDescriptors.read();
        InstrumentationDescriptor customInstrumentationDescriptor =
                CustomInstrumentationBuilder.build(config);
        if (customInstrumentationDescriptor != null) {
            instrumentationDescriptors = new ArrayList<>(instrumentationDescriptors);
            instrumentationDescriptors.add(customInstrumentationDescriptor);
        }

        ConfigServiceFactory configServiceFactory =
                new SimpleConfigServiceFactory(instrumentationDescriptors, InstrumentationConfigBuilder.build(config));

        // need to add instrumentation transformers before initializing telemetry configuration below, since that starts
        // some threads and loads some classes that the instrumentation wants to weave
        EngineModule.createWithSomeDefaults(instrumentation, tmpDir, Global.getThreadContextThreadLocal(),
                instrumentationDescriptors, configServiceFactory, new AgentImpl(), false,
                Collections.singletonList("com.microsoft.applicationinsights.agentc."),
                Collections.singletonList("com.microsoft.applicationinsights.agentc."), agentJarFile);

        String jbossHome = System.getenv("JBOSS_HOME");
        if (!Strings.isNullOrEmpty(jbossHome)) {
            // this is used to delay SSL initialization because SSL initialization triggers loading of
            // java.util.logging (starting with Java 8u231)
            // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
            ApacheSender43.safeToInitLatch = new CountDownLatch(1);
            instrumentation.addTransformer(new JulListeningClassFileTransformer(ApacheSender43.safeToInitLatch));
        }

        TelemetryConfiguration configuration = TelemetryConfiguration.getActiveWithoutInitializingConfig();
        TelemetryConfigurationFactory.INSTANCE.initialize(configuration, buildXmlConfiguration(config));
        FixedRateSampling fixedRateSampling = config.experimental.sampling.fixedRate;
        if (fixedRateSampling != null) {
            addFixedRateSampling(fixedRateSampling, configuration);
        }
        TelemetryClient telemetryClient = new TelemetryClient();
        Global.setDistributedTracingOutboundEnabled(config.experimental.distributedTracing.outboundEnabled);
        Global.setDistributedTracingRequestIdCompatEnabled(
                config.experimental.distributedTracing.requestIdCompatEnabled);
        Global.setTelemetryClient(telemetryClient);
    }

    private static ApplicationInsightsXmlConfiguration buildXmlConfiguration(Configuration config) {

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
        if (!config.experimental.liveMetrics.enabled) {
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

        if (config.experimental.debug) {
            SDKLoggerXmlElement sdkLogger = new SDKLoggerXmlElement();
            sdkLogger.setType("CONSOLE");
            sdkLogger.setLevel("TRACE");
            xmlConfiguration.setSdkLogger(sdkLogger);
        }
        if (config.experimental.developerMode) {
            xmlConfiguration.getChannel().setDeveloperMode(true);
        }
        return xmlConfiguration;
    }

    private static void addFixedRateSampling(FixedRateSampling fixedRateSampling,
                                             TelemetryConfiguration configuration) {

        double samplingRate = MoreObjects.firstNonNull(fixedRateSampling.default_, 100.0);
        Map<Class<?>, Double> samplingPercentages = new HashMap<>();
        if (fixedRateSampling.requests != null) {
            samplingPercentages.put(RequestTelemetry.class, fixedRateSampling.requests);
        }
        if (fixedRateSampling.dependencies != null) {
            samplingPercentages.put(RemoteDependencyTelemetry.class, fixedRateSampling.dependencies);
        }
        if (fixedRateSampling.exceptions != null) {
            samplingPercentages.put(ExceptionTelemetry.class, fixedRateSampling.exceptions);
        }
        if (fixedRateSampling.traces != null) {
            samplingPercentages.put(TraceTelemetry.class, fixedRateSampling.traces);
        }
        if (fixedRateSampling.customEvents != null) {
            samplingPercentages.put(EventTelemetry.class, fixedRateSampling.customEvents);
        }
        if (fixedRateSampling.pageViews != null) {
            samplingPercentages.put(PageViewTelemetry.class, fixedRateSampling.pageViews);
        }
        configuration.getTelemetryProcessors()
                .add(new FixedRateSamplingTelemetryProcessor(samplingRate, samplingPercentages));
    }
}
