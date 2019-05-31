package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.agent.internal.utils.Global;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptor;
import org.glowroot.xyzzy.engine.config.InstrumentationDescriptors;
import org.glowroot.xyzzy.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.xyzzy.engine.impl.SimpleConfigServiceFactory;
import org.glowroot.xyzzy.engine.init.EngineModule;
import org.glowroot.xyzzy.engine.init.MainEntryPointUtil;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            System.out.println("Starting Application Insights Agent v3");
            start(instrumentation, agentJarFile);
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            startupLogger.error("Agent failed to start.", t);
            t.printStackTrace();
        }
    }

    private static void start(Instrumentation instrumentation, File agentJarFile) throws Exception {

        File tmpDir = new File(agentJarFile.getParentFile(), "tmp");

        AgentImpl agent = new AgentImpl(agentJarFile);

        List<InstrumentationDescriptor> instrumentationDescriptors = InstrumentationDescriptors.read();

        ConfigServiceFactory configServiceFactory = new SimpleConfigServiceFactory(instrumentationDescriptors,
                getInstrumentationConfig());

        EngineModule.createWithSomeDefaults(instrumentation, tmpDir, Global.getThreadContextThreadLocal(),
                instrumentationDescriptors, configServiceFactory, agent,
                Collections.singletonList("com.microsoft.applicationinsights."), agentJarFile);
    }

    private static Map<String, Map<String, Object>> getInstrumentationConfig() {

        Map<String, Map<String, Object>> instrumentationConfiguration = new HashMap<>();

        Map<String, Object> servletConfiguration = new HashMap<>();
        servletConfiguration.put("captureRequestServerHostname", true);
        servletConfiguration.put("captureRequestServerPort", true);
        servletConfiguration.put("captureRequestScheme", true);

        Map<String, Object> jdbcConfiguration = new HashMap<>();
        jdbcConfiguration.put("captureBindParametersIncludes", Collections.emptyList());
        jdbcConfiguration.put("captureResultSetNavigate", false);
        jdbcConfiguration.put("captureGetConnection", false);
        jdbcConfiguration.put("explainPlanThresholdMillis", 10000);

        Map<String, Object> springConfiguration = new HashMap<>();
        springConfiguration.put("useAltTransactionNaming", true);

        instrumentationConfiguration.put("servlet", servletConfiguration);
        instrumentationConfiguration.put("jdbc", jdbcConfiguration);
        instrumentationConfiguration.put("spring", springConfiguration);

        return instrumentationConfiguration;
    }
}
