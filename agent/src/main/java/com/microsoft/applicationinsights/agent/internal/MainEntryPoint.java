package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.agent.internal.config.AgentBuiltInConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.config.MethodInfo;
import com.microsoft.applicationinsights.agent.internal.config.builder.XmlAgentConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.internal.channel.common.TransmitterImpl;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.glowroot.xyzzy.engine.config.*;
import org.glowroot.xyzzy.engine.impl.InstrumentationServiceImpl.ConfigServiceFactory;
import org.glowroot.xyzzy.engine.impl.SimpleConfigServiceFactory;
import org.glowroot.xyzzy.engine.init.EngineModule;
import org.glowroot.xyzzy.engine.init.MainEntryPointUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.*;
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

        AgentImpl agent = new AgentImpl(agentJarFile);

        AgentConfiguration agentConfiguration =
                new XmlAgentConfigurationBuilder().parseConfigurationFile(agentJarParentFile.getAbsolutePath());

        AgentBuiltInConfiguration builtInConfiguration = agentConfiguration.getBuiltInConfiguration();

        if (!builtInConfiguration.isEnabled()) {
            // TODO this has consequences if app is using AI SDK
            return;
        }

        Global.isW3CEnabled = builtInConfiguration.isW3cEnabled();
        Global.isW3CBackportEnabled = builtInConfiguration.isW3CBackportEnabled();

        List<InstrumentationDescriptor> instrumentationDescriptors = getInstrumentationDescriptors(agentConfiguration);

        ConfigServiceFactory configServiceFactory = new SimpleConfigServiceFactory(instrumentationDescriptors,
                getInstrumentationConfig(builtInConfiguration));

        final EngineModule engineModule = EngineModule
                .createWithSomeDefaults(instrumentation, tmpDir, Global.getThreadContextThreadLocal(),
                        instrumentationDescriptors, configServiceFactory, agent,
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

    private static List<InstrumentationDescriptor> getInstrumentationDescriptors(AgentConfiguration agentConfiguration)
            throws IOException {

        AgentBuiltInConfiguration builtInConfiguration = agentConfiguration.getBuiltInConfiguration();
        boolean httpEnabled = builtInConfiguration.isHttpEnabled();
        boolean jdbcEnabled = builtInConfiguration.isJdbcEnabled();
        boolean redisEnabled = builtInConfiguration.isRedisEnabled();

        List<InstrumentationDescriptor> instrumentationDescriptors = new ArrayList<>();
        for (InstrumentationDescriptor instrumentationDescriptor : InstrumentationDescriptors.read()) {
            switch (instrumentationDescriptor.name()) {
                case "apache-http-client":
                case "okhttp":
                    if (httpEnabled) {
                        instrumentationDescriptors.add(instrumentationDescriptor);
                    }
                    break;
                case "jdbc":
                    if (jdbcEnabled) {
                        instrumentationDescriptors.add(instrumentationDescriptor);
                    }
                    break;
                case "redis":
                    if (redisEnabled) {
                        instrumentationDescriptors.add(instrumentationDescriptor);
                    }
                    break;
                default:
                    instrumentationDescriptors.add(instrumentationDescriptor);
                    break;
            }
        }

        InstrumentationDescriptor instrumentationDescriptor = buildCustomInstrumentation(agentConfiguration);
        if (instrumentationDescriptor != null) {
            // need to test before enabling this
            // instrumentationDescriptors.add(instrumentationDescriptor);
        }
        return instrumentationDescriptors;
    }

    private static InstrumentationDescriptor buildCustomInstrumentation(AgentConfiguration agentConfiguration) {

        List<AdviceConfig> adviceConfigs = new ArrayList<>();

        for (Map.Entry<String, ClassInstrumentationData> entry : agentConfiguration.getClassesToInstrument()
                .entrySet()) {

            String className = entry.getKey().replace('/', '.');
            ClassInstrumentationData classInstrumentationData = entry.getValue();

            MethodInfo allClassMethods = classInstrumentationData.getAllClassMethods();

            if (allClassMethods == null) {

                for (Map.Entry<String, Map<String, MethodInfo>> entry2 :
                        classInstrumentationData.getMethodInfos().entrySet()) {

                    String methodName = entry2.getKey();
                    Map<String, MethodInfo> value = entry2.getValue();

                    for (Map.Entry<String, MethodInfo> entry3 : value.entrySet()) {

                        MethodInfo methodInfo = entry3.getValue();

                        if (methodInfo.isReportCaughtExceptions()) {
                            InternalLogger.INSTANCE.warn("reportCaughtExceptions attribute is no longer supported");
                        }
                        if (methodInfo.isReportExecutionTime()) {

                            ImmutableAdviceConfig.Builder builder = ImmutableAdviceConfig.builder()
                                    .className(className)
                                    .methodName(methodName);

                            String signature = entry3.getKey();

                            if (!signature.equals(ClassInstrumentationData.ANY_SIGNATURE_MARKER)) {
                                // TODO parse signature and call addAllMethodParameterTypes() and methodReturnType()
                                InternalLogger.INSTANCE.warn("signature attribute is not currently supported");
                            }

                            adviceConfigs.add(builder
                                    // xyzzy doesn't support threshold, so threshold is embedded into message and
                                    // then parsed out by the agent to decide whether to report telemetry
                                    .spanMessageTemplate(
                                            "{{className}}.{{methodName}}#" + classInstrumentationData.getClassType() +
                                                    ":" + methodInfo.getThresholdInMS())
                                    .build());
                        }
                    }
                }
            } else {
                adviceConfigs.add(ImmutableAdviceConfig.builder()
                        .className(className)
                        .methodName("*")
                        .build());
            }
        }

        if (adviceConfigs.isEmpty()) {
            return null;
        } else {
            return ImmutableInstrumentationDescriptor.builder()
                    .addAllAdviceConfigs(adviceConfigs)
                    .build();
        }
    }

    private static Map<String, Map<String, Object>> getInstrumentationConfig(
            AgentBuiltInConfiguration builtInConfiguration) {

        Map<String, Map<String, Object>> instrumentationConfiguration = new HashMap<>();

        Map<String, Object> servletConfiguration = new HashMap<>();
        servletConfiguration.put("captureRequestServerHostname", true);
        servletConfiguration.put("captureRequestServerPort", true);
        servletConfiguration.put("captureRequestScheme", true);

        Map<String, Object> jdbcConfiguration = new HashMap<>();
        jdbcConfiguration.put("captureBindParametersIncludes", Collections.emptyList());
        jdbcConfiguration.put("captureResultSetNavigate", false);
        jdbcConfiguration.put("captureGetConnection", false);
        jdbcConfiguration.put("explainPlanThresholdMillis", builtInConfiguration.getQueryPlanThresholdInMS());

        Map<String, Object> springConfiguration = new HashMap<>();
        springConfiguration.put("useAltTransactionNaming", true);

        instrumentationConfiguration.put("servlet", servletConfiguration);
        instrumentationConfiguration.put("jdbc", jdbcConfiguration);
        instrumentationConfiguration.put("spring", springConfiguration);

        return instrumentationConfiguration;
    }
}
