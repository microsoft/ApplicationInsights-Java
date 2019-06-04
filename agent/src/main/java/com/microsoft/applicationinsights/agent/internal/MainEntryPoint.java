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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.config.BuiltInInstrumentation;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.internal.channel.common.TransmitterImpl;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.xyzzy.engine.impl.SimpleConfigServiceFactory;
import org.glowroot.xyzzy.engine.init.EngineModule;
import org.glowroot.xyzzy.engine.init.MainEntryPointUtil;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainEntryPoint {

    private MainEntryPoint() {
    }

    public static void premain(Instrumentation instrumentation, File agentJarFile) {
        Logger startupLogger;
        try {
            startupLogger = MainEntryPointUtil.initLogging("com.microsoft.applicationinsights.agent", instrumentation);
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            System.err.println("Agent failed to start: " + t.getMessage());
            t.printStackTrace();
            return;
        }

        try {
            start(instrumentation, agentJarFile);
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            startupLogger.error("Agent failed to start.", t);
            t.printStackTrace();
        }
    }

    private static void start(Instrumentation instrumentation, File agentJarFile) throws Exception {

        File agentJarParentFile = agentJarFile.getParentFile();
        File javaTmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpDir = new File(javaTmpDir, "ai-java");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new Exception("Could not create directory: " + tmpDir.getAbsolutePath());
        }

        Global.setTelemetryClient(new TelemetryClient(ApplicationInsightsXmlLoader.load(agentJarFile)));

        AgentConfiguration agentConfiguration = AIAgentXmlLoader.load(agentJarParentFile);

        BuiltInInstrumentation builtInInstrumentation = agentConfiguration.getBuiltInInstrumentation();

        if (!builtInInstrumentation.isEnabled()) {
            // TODO this has consequences if app is using AI SDK
            return;
        }

        Global.isOutboundW3CEnabled = builtInInstrumentation.isW3cEnabled();
        Global.isOutboundW3CBackportEnabled = builtInInstrumentation.isW3CBackportEnabled();

        List<InstrumentationDescriptor> instrumentationDescriptors =
                AIAgentXmlLoader.getInstrumentationDescriptors(agentConfiguration);

        ConfigServiceFactory configServiceFactory = new SimpleConfigServiceFactory(instrumentationDescriptors,
                AIAgentXmlLoader.getInstrumentationConfig(builtInInstrumentation));

        final EngineModule engineModule = EngineModule
                .createWithSomeDefaults(instrumentation, tmpDir, Global.getThreadContextThreadLocal(),
                        instrumentationDescriptors, configServiceFactory, new AgentImpl(),
                        Collections.singletonList("com.microsoft.applicationinsights."), agentJarFile);

        ThreadFactory threadFactory = ThreadPoolUtils.createDaemonThreadFactory(TransmitterImpl.class);
        Executors.newSingleThreadScheduledExecutor(threadFactory)
                .scheduleWithFixedDelay(new Runnable() {
                    @Override public void run() {
                        engineModule.getPreloadSomeSuperTypesCache().writeToFileAsync();
                    }
                }, 5, 5, SECONDS);

        instrumentation.addTransformer(new SpringApplicationClassFileTransformer());
    }
}
