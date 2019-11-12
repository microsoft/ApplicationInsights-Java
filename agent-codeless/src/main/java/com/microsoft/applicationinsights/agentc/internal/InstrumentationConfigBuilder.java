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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentationConfigBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CustomInstrumentationBuilder.class);

    static List<InstrumentationDescriptor> buildDescriptors(Configuration configuration) throws IOException,
            ClassNotFoundException {

        List<InstrumentationDescriptor> instrumentationDescriptors = new ArrayList<>();
        Map<String, Map<String, Object>> instrumentationMapMap = configuration.experimental.instrumentation;
        for (InstrumentationDescriptor instrumentationDescriptor : InstrumentationDescriptors.read()) {
            if (isEnabled(instrumentationMapMap, instrumentationDescriptor.id())) {
                instrumentationDescriptors.add(instrumentationDescriptor);
            }
        }
        InstrumentationDescriptor customInstrumentationDescriptor = CustomInstrumentationBuilder.build(configuration);
        if (customInstrumentationDescriptor != null) {
            instrumentationDescriptors = new ArrayList<>(instrumentationDescriptors);
            instrumentationDescriptors.add(customInstrumentationDescriptor);
        }
        return instrumentationDescriptors;
    }

    static Map<String, Map<String, Object>> build(Configuration configuration) {

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

    private static boolean isEnabled(Map<String, Map<String, Object>> instrumentationMapMap, String instrumentationId) {
        Map<String, Object> instrumentationMap = instrumentationMapMap.get(instrumentationId);
        if (instrumentationMap == null) {
            return true;
        }
        Object enabled = instrumentationMap.get("enabled");
        if (enabled == null) {
            return true;
        }
        if (enabled instanceof Boolean) {
            return (boolean) enabled;
        } else {
            logger.warn("enabled must be a boolean, but found: {}", enabled.getClass());
            return true;
        }
    }

    @Nullable
    private static Number getExplainPlanThresholdInMS(Configuration configuration) {
        Map<String, Object> jdbc = configuration.experimental.instrumentation.get("jdbc");
        if (jdbc == null) {
            return null;
        }
        Object explainPlanThresholdInMS = jdbc.get("explainPlanThresholdInMS");
        if (explainPlanThresholdInMS == null) {
            return null;
        }
        if (!(explainPlanThresholdInMS instanceof Number)) {
            logger.warn("jdbc explainPlanThresholdMillis must be a number, but found: {}",
                    explainPlanThresholdInMS.getClass());
            return null;
        }
        return (Number) explainPlanThresholdInMS;
    }

    @Nullable
    private static LoggingThreshold getLoggingThreshold(Configuration configuration) {
        Map<String, Object> logging = configuration.experimental.instrumentation.get("logging");
        if (logging == null) {
            return null;
        }
        Object thresholdObj = logging.get("threshold");
        if (thresholdObj == null) {
            return null;
        }
        if (!(thresholdObj instanceof String)) {
            logger.warn("logging threshold must be a string, but found: {}", thresholdObj.getClass());
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
                logger.warn("invalid logging threshold: {}", threshold);
                return null;
        }
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
