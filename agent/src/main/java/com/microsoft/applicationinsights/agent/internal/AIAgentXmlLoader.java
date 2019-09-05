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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.BuiltInInstrumentation;
import com.microsoft.applicationinsights.agent.internal.config.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.config.MethodInfo;
import com.microsoft.applicationinsights.agent.internal.config.builder.XmlAgentConfigurationBuilder;
import org.glowroot.instrumentation.engine.config.AdviceConfig;
import org.glowroot.instrumentation.engine.config.AdviceConfig.CaptureKind;
import org.glowroot.instrumentation.engine.config.ImmutableAdviceConfig;
import org.glowroot.instrumentation.engine.config.ImmutableInstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptors;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AIAgentXmlLoader {

    private static final Logger logger = LoggerFactory.getLogger(AIAgentXmlLoader.class);

    static AgentConfiguration load(File agentJarParentFile) {
        return new XmlAgentConfigurationBuilder().parseConfigurationFile(agentJarParentFile.getAbsolutePath());
    }

    static List<InstrumentationDescriptor> getInstrumentationDescriptors(AgentConfiguration agentConfiguration)
            throws IOException {

        BuiltInInstrumentation builtInConfiguration = agentConfiguration.getBuiltInInstrumentation();
        boolean httpEnabled = builtInConfiguration.isHttpEnabled();
        boolean jdbcEnabled = builtInConfiguration.isJdbcEnabled();
        boolean loggingEnabled = builtInConfiguration.isLoggingEnabled();
        boolean redisEnabled = builtInConfiguration.isJedisEnabled();

        List<InstrumentationDescriptor> instrumentationDescriptors = new ArrayList<>();
        for (InstrumentationDescriptor instrumentationDescriptor : InstrumentationDescriptors.read()) {
            switch (instrumentationDescriptor.id()) {
                case "apache-http-client":
                case "http-url-connection":
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
                case "log4j":
                case "logback":
                    if (loggingEnabled) {
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
            instrumentationDescriptors.add(instrumentationDescriptor);
        }
        return instrumentationDescriptors;
    }

    static Map<String, Map<String, Object>> getInstrumentationConfig(BuiltInInstrumentation builtInConfiguration) {

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

        // must be one of trace, debug, info, warn, error (which are supported by both log4j and logback)
        String loggingThreshold = builtInConfiguration.getLoggingThreshold();

        Map<String, Object> log4jConfiguration = new HashMap<>();
        log4jConfiguration.put("threshold", loggingThreshold);

        Map<String, Object> logbackConfiguration = new HashMap<>();
        logbackConfiguration.put("threshold", loggingThreshold);

        instrumentationConfiguration.put("servlet", servletConfiguration);
        instrumentationConfiguration.put("jdbc", jdbcConfiguration);
        instrumentationConfiguration.put("log4j", log4jConfiguration);
        instrumentationConfiguration.put("logback", logbackConfiguration);

        return instrumentationConfiguration;
    }


    private static InstrumentationDescriptor buildCustomInstrumentation(AgentConfiguration agentConfiguration) {

        List<AdviceConfig> adviceConfigs = new ArrayList<>();

        for (Map.Entry<String, ClassInstrumentationData> classEntry : agentConfiguration.getClassesToInstrument()
                .entrySet()) {

            String className = classEntry.getKey();
            if (!validJavaFqcn(className)) {
                // this is needed to prevent glowroot wildcard from being used for now
                // and also to prevent commas in the class name which would cause parsing issues in LocalSpanImpl
                logger.warn("Invalid class name: {}", className);
                continue;
            }
            ClassInstrumentationData classInstrumentationData = classEntry.getValue();

            for (Map.Entry<String, Map<String, MethodInfo>> methodNameEntry :
                    classInstrumentationData.getMethodInfos().entrySet()) {

                String methodName = methodNameEntry.getKey();
                if (!validJavaIdentifier(methodName)) {
                    // this is needed to prevent glowroot wildcard from being used for now
                    // and also to prevent commas in the method name which would cause parsing issues in LocalSpanImpl
                    logger.warn("Invalid method name: {}", methodName);
                    continue;
                }

                Map<String, MethodInfo> methodInfos = methodNameEntry.getValue();

                for (Map.Entry<String, MethodInfo> entry : methodInfos.entrySet()) {

                    MethodInfo methodInfo = entry.getValue();
                    String signature = entry.getKey();

                    ImmutableAdviceConfig.Builder adviceConfig = ImmutableAdviceConfig.builder()
                            .className(className)
                            .methodName(methodName);

                    if (signature.equals(ClassInstrumentationData.ANY_SIGNATURE_MARKER)) {
                        adviceConfig.addMethodParameterTypes("..");
                    } else {
                        Method method = new Method(methodName, signature);
                        for (Type type : method.getArgumentTypes()) {
                            adviceConfig.addMethodParameterTypes(type.getClassName());
                        }
                        adviceConfig.methodReturnType(method.getReturnType().getClassName());
                    }

                    adviceConfigs.add(adviceConfig.captureKind(CaptureKind.LOCAL_SPAN)
                            // advice config doesn't support threshold, so threshold is embedded into message
                            // and then parsed out by the agent to decide whether to report telemetry
                            .spanMessageTemplate(
                                    "__custom," + className + "," + methodName + "," + methodInfo.getThresholdInMS() +
                                            "," + classInstrumentationData.getClassType())
                            .timerName("custom")
                            .build());
                }
            }
        }

        if (adviceConfigs.isEmpty()) {
            return null;
        } else {
            return ImmutableInstrumentationDescriptor.builder()
                    .id("__custom")
                    .name("__custom")
                    .addAllAdviceConfigs(adviceConfigs)
                    .build();
        }
    }

    @VisibleForTesting
    static boolean validJavaFqcn(String fqcn) {
        List<String> parts = Splitter.on('.').splitToList(fqcn);
        if (parts.isEmpty()) {
            return false;
        }
        for (int i = 0; i < parts.size() - 1; i++) {
            if (!validJavaIdentifier(parts.get(i))) {
                return false;
            }
        }
        return validJavaIdentifier(parts.get(parts.size() - 1));
    }

    @VisibleForTesting
    static boolean validJavaIdentifier(String identifier) {
        if (identifier.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }
        for (int i = 1; i < identifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
