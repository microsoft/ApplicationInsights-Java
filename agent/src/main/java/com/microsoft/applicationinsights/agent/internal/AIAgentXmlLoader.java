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

import com.microsoft.applicationinsights.agent.internal.config.BuiltInInstrumentation;
import com.microsoft.applicationinsights.agent.internal.config.XmlAgentConfigurationBuilder;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptors;

class AIAgentXmlLoader {

    static BuiltInInstrumentation load(File agentJarParentFile) {
        return new XmlAgentConfigurationBuilder().parseConfigurationFile(agentJarParentFile.getAbsolutePath());
    }

    static List<InstrumentationDescriptor> getInstrumentationDescriptors(
            BuiltInInstrumentation builtInInstrumentation) throws IOException {

        boolean httpEnabled = builtInInstrumentation.isHttpEnabled();
        boolean jdbcEnabled = builtInInstrumentation.isJdbcEnabled();
        boolean loggingEnabled = builtInInstrumentation.isLoggingEnabled();
        boolean redisEnabled = builtInInstrumentation.isJedisEnabled();

        List<InstrumentationDescriptor> instrumentationDescriptors = new ArrayList<>();
        for (InstrumentationDescriptor instrumentationDescriptor : InstrumentationDescriptors.read()) {
            switch (instrumentationDescriptor.name()) {
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

        instrumentationConfiguration.put("servlet", servletConfiguration);
        instrumentationConfiguration.put("jdbc", jdbcConfiguration);

        return instrumentationConfiguration;
    }
}
