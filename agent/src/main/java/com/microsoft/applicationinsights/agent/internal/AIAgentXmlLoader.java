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

import com.microsoft.applicationinsights.agent.internal.config.AgentBuiltInConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.config.MethodInfo;
import com.microsoft.applicationinsights.agent.internal.config.builder.XmlAgentConfigurationBuilder;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.glowroot.xyzzy.engine.config.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

class AIAgentXmlLoader {

    static AgentConfiguration load(File agentJarParentFile) {
        return new XmlAgentConfigurationBuilder().parseConfigurationFile(agentJarParentFile.getAbsolutePath());
    }

    static List<InstrumentationDescriptor> getInstrumentationDescriptors(AgentConfiguration agentConfiguration)
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

    static Map<String, Map<String, Object>> getInstrumentationConfig(AgentBuiltInConfiguration builtInConfiguration) {

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
}
