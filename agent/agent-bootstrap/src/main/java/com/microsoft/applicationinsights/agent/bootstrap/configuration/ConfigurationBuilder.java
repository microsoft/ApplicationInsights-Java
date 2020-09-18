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

package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.FixedRateSampling;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.JmxMetric;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.PreviewConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.Sampling;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationBuilder {

    private static final String APPLICATIONINSIGHTS_CONFIGURATION_CONTENT = "APPLICATIONINSIGHTS_CONFIGURATION_CONTENT";
    private static final String APPLICATIONINSIGHTS_CONFIGURATION_FILE = "APPLICATIONINSIGHTS_CONFIGURATION_FILE";
    private static final String APPLICATIONINSIGHTS_JMX_METRICS = "APPLICATIONINSIGHTS_JMX_METRICS";
    private static final String APPLICATIONINSIGHTS_LOG_CAPTURE_THRESHOLD = "APPLICATIONINSIGHTS_LOG_CAPTURE_THRESHOLD";
    private static final String APPLICATIONINSIGHTS_SAMPLING_RATE = "APPLICATIONINSIGHTS_SAMPLING_RATE";

    private static final String APPLICATIONINSIGHTS_ROLE_NAME = "APPLICATIONINSIGHTS_ROLE_NAME";
    private static final String APPLICATIONINSIGHTS_ROLE_INSTANCE = "APPLICATIONINSIGHTS_ROLE_INSTANCE";

    private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
    private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";

    // cannot use logger before loading configuration, so need to store any messages locally until logger is initialized
    private static final List<ConfigurationMessage> configurationMessages = new CopyOnWriteArrayList<>();

    public static Configuration create(Path agentJarPath) throws IOException {

        Configuration config = loadConfigurationFile(agentJarPath);
        PreviewConfiguration preview = config.instrumentationSettings.preview;

        preview.roleName = overlayWithEnvVars(APPLICATIONINSIGHTS_ROLE_NAME, WEBSITE_SITE_NAME, preview.roleName);
        preview.roleInstance =
                overlayWithEnvVars(APPLICATIONINSIGHTS_ROLE_INSTANCE, WEBSITE_INSTANCE_ID, preview.roleInstance);

        loadSamplingRateEnvVar(preview);
        loadLogCaptureEnvVar(preview);
        loadJmxMetrics(preview);

        return config;
    }

    private static void loadSamplingRateEnvVar(PreviewConfiguration preview) {
        final String samplingRate = overlayWithEnvVar(APPLICATIONINSIGHTS_SAMPLING_RATE, "100");
        if (!samplingRate.equals("100")) {
            FixedRateSampling fixedRateSampling = new FixedRateSampling();
            fixedRateSampling.percentage = Double.parseDouble(samplingRate);
            Sampling sampling = new Sampling();
            sampling.fixedRate = fixedRateSampling;
            preview.sampling = sampling;
        }
    }

    private static void loadLogCaptureEnvVar(PreviewConfiguration preview) {
        final String logCaptureThreshold = overlayWithEnvVar(APPLICATIONINSIGHTS_LOG_CAPTURE_THRESHOLD, "INFO");
        if (!logCaptureThreshold.equals("INFO")) {
            Map<String, Object> logging = preview.instrumentation.get("logging");
            if (logging == null) {
                logging = new HashMap<>();
            }
            logging.put("threshold", logCaptureThreshold);
        }
    }

    private static void loadJmxMetrics(PreviewConfiguration preview) throws IOException {
        String jmxMetricsEnvVarJson = overlayWithEnvVar(APPLICATIONINSIGHTS_JMX_METRICS, null);

        // JmxMetrics env variable has higher precedence over jmxMetrics config from ApplicationInsights.json
        if (jmxMetricsEnvVarJson != null && !jmxMetricsEnvVarJson.isEmpty()) {
            Moshi moshi = new Moshi.Builder().build();
            Type listOfJmxMetrics = Types.newParameterizedType(List.class, JmxMetric.class);
            JsonReader reader = JsonReader.of(new Buffer().writeUtf8(jmxMetricsEnvVarJson));
            reader.setLenient(true);
            JsonAdapter<List<JmxMetric>> jsonAdapter = moshi.adapter(listOfJmxMetrics);
            preview.jmxMetrics = jsonAdapter.fromJson(reader);
        }
        if (!jmxMetricExisted(preview.jmxMetrics, "java.lang:type=Threading", "ThreadCount")) {
            JmxMetric threadCountJmxMetric = new JmxMetric();
            threadCountJmxMetric.objectName = "java.lang:type=Threading";
            threadCountJmxMetric.attribute = "ThreadCount";
            threadCountJmxMetric.display = "Current Thread Count";
            preview.jmxMetrics.add(threadCountJmxMetric);
        }
        if (!jmxMetricExisted(preview.jmxMetrics, "java.lang:type=ClassLoading", "LoadedClassCount")) {
            JmxMetric classCountJmxMetric = new JmxMetric();
            classCountJmxMetric.objectName = "java.lang:type=ClassLoading";
            classCountJmxMetric.attribute = "LoadedClassCount";
            classCountJmxMetric.display = "Loaded Class Count";
            preview.jmxMetrics.add(classCountJmxMetric);
        }
    }

    private static boolean jmxMetricExisted(List<InstrumentationSettings.JmxMetric> jmxMetrics, String objectName, String attribute) {
        for (JmxMetric metric : jmxMetrics) {
            if (metric.objectName.equals(objectName) && metric.attribute.equals(attribute)) {
                return true;
            }
        }
        return false;
    }

    private static Configuration loadConfigurationFile(Path agentJarPath) throws IOException {
        String configurationContent = System.getenv(APPLICATIONINSIGHTS_CONFIGURATION_CONTENT);
        if (configurationContent != null && !configurationContent.isEmpty()) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
            return jsonAdapter.fromJson(configurationContent);
        }

        if (DiagnosticsHelper.isAnyCodelessAttach()) {
            // codeless attach only supports configuration via environment variables (for now at least)
            return new Configuration();
        }

        Path configPath;
        boolean warnIfMissing;
        String configPathStr = getEnvVarOrProperty(APPLICATIONINSIGHTS_CONFIGURATION_FILE, "applicationinsights.configurationFile");
        if (configPathStr == null) {
            configPath = agentJarPath.resolveSibling("ApplicationInsights.json");
            warnIfMissing = false;
        } else {
            configPath = agentJarPath.resolveSibling(configPathStr);
            warnIfMissing = true;
        }

        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
                Buffer buffer = new Buffer();
                buffer.readFrom(in);
                try {
                    return jsonAdapter.fromJson(buffer);
                } catch (Exception e) {
                    throw new ConfigurationException(
                            "Error parsing configuration file: " + configPath.toAbsolutePath().toString(), e);
                }
            }
        } else {
            if (warnIfMissing) {
                configurationMessages.add(new ConfigurationMessage("could not find configuration file: {}", configPathStr));
            }
            return new Configuration();
        }
    }

    public static void logConfigurationMessages() {
        Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);
        for (ConfigurationMessage configurationMessage : configurationMessages) {
            configurationMessage.log(logger);
        }
    }

    // never returns empty string (empty string is normalized to null)
    private static String getEnvVar(String name) {
        return trimAndEmptyToNull(System.getenv(name));
    }

    // never returns empty string (empty string is normalized to null)
    private static String getEnvVarOrProperty(String envVarName, String propertyName) {
        String value = trimAndEmptyToNull(System.getenv(envVarName));
        return value != null ? value : trimAndEmptyToNull(System.getProperty(propertyName));
    }

    // visible for testing
    static String overlayWithEnvVars(String name1, String name2, String defaultValue) {
        String value = getEnv(name1);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = getEnv(name2);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }

    static String overlayWithEnvVar(String name, String defaultValue) {
        String value = getEnv(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        return defaultValue;
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        // TODO is the best way to identify running as Azure Functions worker?
        // TODO is this the correct way to match role name from Azure Functions IIS host?
        if (name.equals("WEBSITE_SITE_NAME") && "java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
            // special case for Azure Functions
            value = value.toLowerCase(Locale.ENGLISH);
        }
        return value;
    }

    // visible for testing
    static Map<String, String> overlayWithEnvVars(String name, Map<String, String> defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            Map<String, String> stringMap = new HashMap<>();
            Map<String, Object> objectMap;
            try {
                objectMap = adapter.fromJson(value);
            } catch (Exception e) {
                configurationMessages.add(new ConfigurationMessage("could not parse environment variable {} as json: {}", name, value));
                return defaultValue;
            }
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof String)) {
                    configurationMessages.add(new ConfigurationMessage("currently only string values are supported in json map from {}: {}", name, value));
                    return defaultValue;
                }
                stringMap.put(entry.getKey(), (String) val);
            }
            return stringMap;
        }
        return defaultValue;
    }

    // visible for testing
    static String trimAndEmptyToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class ConfigurationException extends RuntimeException {

        public ConfigurationException(String message) {
            super(message);
        }

        ConfigurationException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class ConfigurationMessage {
        private final String message;
        private final Object[] args;

        public ConfigurationMessage(String message, Object... args) {
            this.message = message;
            this.args = args;
        }

        private void log(Logger logger) {
            logger.warn(message, args);
        }
    }
}
