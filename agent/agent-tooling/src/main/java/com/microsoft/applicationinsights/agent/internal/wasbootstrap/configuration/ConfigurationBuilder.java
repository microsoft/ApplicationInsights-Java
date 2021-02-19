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

package com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonEncodingException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationBuilder {

    private static final String APPLICATIONINSIGHTS_CONFIGURATION_FILE = "APPLICATIONINSIGHTS_CONFIGURATION_FILE";

    private static final String APPLICATIONINSIGHTS_CONNECTION_STRING = "APPLICATIONINSIGHTS_CONNECTION_STRING";

    // this is for backwards compatibility only
    private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";

    private static final String APPLICATIONINSIGHTS_ROLE_NAME = "APPLICATIONINSIGHTS_ROLE_NAME";
    private static final String APPLICATIONINSIGHTS_ROLE_INSTANCE = "APPLICATIONINSIGHTS_ROLE_INSTANCE";

    private static final String APPLICATIONINSIGHTS_JMX_METRICS = "APPLICATIONINSIGHTS_JMX_METRICS";
    private static final String APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE = "APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE";

    private static final String APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL";

    private static final String APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL = "APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL";

    private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
    private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";

    // cannot use logger before loading configuration, so need to store any messages locally until logger is initialized
    private static final List<ConfigurationMessage> configurationMessages = new CopyOnWriteArrayList<>();

    public static Configuration create(Path agentJarPath) throws IOException {
        Configuration config = loadConfigurationFile(agentJarPath);
        overlayEnvVars(config);
        return config;
    }

    private static void loadLogCaptureEnvVar(Configuration config) {
        String loggingEnvVar = getEnvVar(APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL);
        if (loggingEnvVar != null) {
            config.instrumentation
                    .computeIfAbsent("logging", k -> new HashMap<>())
                    .put("level", loggingEnvVar);
        }
    }

    private static void loadJmxMetricsEnvVar(Configuration config) throws IOException {
        String jmxMetricsEnvVarJson = getEnvVar(APPLICATIONINSIGHTS_JMX_METRICS);

        // JmxMetrics env variable has higher precedence over jmxMetrics config from applicationinsights.json
        if (jmxMetricsEnvVarJson != null && !jmxMetricsEnvVarJson.isEmpty()) {
            Moshi moshi = MoshiBuilderFactory.createBasicBuilder();
            Type listOfJmxMetrics = Types.newParameterizedType(List.class, JmxMetric.class);
            JsonReader reader = JsonReader.of(new Buffer().writeUtf8(jmxMetricsEnvVarJson));
            reader.setLenient(true);
            JsonAdapter<List<JmxMetric>> jsonAdapter = moshi.adapter(listOfJmxMetrics);
            config.jmxMetrics = jsonAdapter.fromJson(reader);
        }
    }

    private static void addDefaultJmxMetricsIfNotPresent(Configuration config) {
        if (!jmxMetricExisted(config.jmxMetrics, "java.lang:type=Threading", "ThreadCount")) {
            JmxMetric threadCountJmxMetric = new JmxMetric();
            threadCountJmxMetric.name = "Current Thread Count";
            threadCountJmxMetric.objectName = "java.lang:type=Threading";
            threadCountJmxMetric.attribute = "ThreadCount";
            config.jmxMetrics.add(threadCountJmxMetric);
        }
        if (!jmxMetricExisted(config.jmxMetrics, "java.lang:type=ClassLoading", "LoadedClassCount")) {
            JmxMetric classCountJmxMetric = new JmxMetric();
            classCountJmxMetric.name = "Loaded Class Count";
            classCountJmxMetric.objectName = "java.lang:type=ClassLoading";
            classCountJmxMetric.attribute = "LoadedClassCount";
            config.jmxMetrics.add(classCountJmxMetric);
        }
    }

    private static boolean jmxMetricExisted(List<Configuration.JmxMetric> jmxMetrics, String objectName, String attribute) {
        for (JmxMetric metric : jmxMetrics) {
            if (metric.objectName.equals(objectName) && metric.attribute.equals(attribute)) {
                return true;
            }
        }
        return false;
    }

    private static void loadInstrumentationEnabledEnvVars(Configuration config) {
        loadInstrumentationEnabledEnvVar(config, "micrometer", "APPLICATIONINSIGHTS_INSTRUMENTATION_MICROMETER_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "jdbc", "APPLICATIONINSIGHTS_INSTRUMENTATION_JDBC_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "redis", "APPLICATIONINSIGHTS_INSTRUMENTATION_REDIS_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "kafka", "APPLICATIONINSIGHTS_INSTRUMENTATION_KAFKA_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "jms", "APPLICATIONINSIGHTS_INSTRUMENTATION_JMS_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "mongo", "APPLICATIONINSIGHTS_INSTRUMENTATION_MONGO_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "cassandra", "APPLICATIONINSIGHTS_INSTRUMENTATION_CASSANDRA_ENABLED");
        loadInstrumentationEnabledEnvVar(config, "spring-scheduling", "APPLICATIONINSIGHTS_INSTRUMENTATION_SPRING_SCHEDULING_ENABLED");
    }

    private static void loadInstrumentationEnabledEnvVar(Configuration config, String instrumentationName, String envVarName) {
        String instrumentationEnabledEnvVar = getEnvVar(envVarName);
        if (instrumentationEnabledEnvVar != null) {
            Map<String, Object> properties = config.instrumentation.computeIfAbsent(instrumentationName, k -> new HashMap<>());
            // intentionally allowing NumberFormatException to bubble up as invalid configuration and prevent agent from starting
            properties.put("enabled", Boolean.parseBoolean(instrumentationEnabledEnvVar));
        }
    }

    private static Configuration loadConfigurationFile(Path agentJarPath) throws IOException {
        if (DiagnosticsHelper.isAnyCodelessAttach()) {
            // codeless attach only supports configuration via environment variables (for now at least)
            return new Configuration();
        }

        String configPathStr = getConfigPath();
        if (configPathStr != null) {
            Path configPath = agentJarPath.resolveSibling(configPathStr);
            if (Files.exists(configPath)) {
                return loadJsonConfigFile(configPath);
            } else {
                // fail fast any time configuration is invalid
                throw new IllegalStateException("could not find requested configuration file: " + configPathStr);
            }
        }

        Path configPath = agentJarPath.resolveSibling("applicationinsights.json");
        if (Files.exists(configPath)) {
            return loadJsonConfigFile(configPath);
        }

        if (Files.exists(agentJarPath.resolveSibling("ApplicationInsights.json"))) {
            throw new IllegalStateException("found ApplicationInsights.json, but it should be lowercase: applicationinsights.json");
        }

        // json configuration file is not required, ok to configure via env var alone
        return new Configuration();
    }

    public static void logConfigurationMessages() {
        Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);
        for (ConfigurationMessage configurationMessage : configurationMessages) {
            configurationMessage.log(logger);
        }
    }

    public static void overlayEnvVars(Configuration config) throws IOException {
        config.connectionString = overlayWithEnvVar(APPLICATIONINSIGHTS_CONNECTION_STRING, config.connectionString);
        if (config.connectionString == null) {
            // this is for backwards compatibility only
            String instrumentationKey = System.getenv(APPINSIGHTS_INSTRUMENTATIONKEY);
            if (instrumentationKey != null && !instrumentationKey.isEmpty()) {
                // TODO log an info message recommending APPLICATIONINSIGHTS_CONNECTION_STRING
                config.connectionString = "InstrumentationKey=" + instrumentationKey;
            }
        }

        if (isTrimEmpty(config.role.name)) {
            // only use WEBSITE_SITE_NAME as a fallback
            config.role.name = getEnvVar(WEBSITE_SITE_NAME);
        }
        config.role.name = overlayWithEnvVar(APPLICATIONINSIGHTS_ROLE_NAME, config.role.name);

        if (isTrimEmpty(config.role.instance)) {
            // only use WEBSITE_INSTANCE_ID as a fallback
            config.role.instance = getEnvVar(WEBSITE_INSTANCE_ID);
        }
        config.role.instance = overlayWithEnvVar(APPLICATIONINSIGHTS_ROLE_INSTANCE, config.role.instance);

        config.sampling.percentage = overlayWithEnvVar(APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE, config.sampling.percentage);

        config.selfDiagnostics.level = overlayWithEnvVar(APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL, config.selfDiagnostics.level);

        loadLogCaptureEnvVar(config);
        loadJmxMetricsEnvVar(config);

        addDefaultJmxMetricsIfNotPresent(config);

        loadInstrumentationEnabledEnvVars(config);
    }

    private static String getConfigPath() {
        String value = getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_FILE);
        if (value != null) {
            return value;
        }
        // intentionally not checking system properties for other system properties
        // with the intention to keep configuration paths minimal to help with supportability
        return trimAndEmptyToNull(System.getProperty("applicationinsights.configuration.file"));
    }

    private static String getWebsiteSiteNameEnvVar() {
        String value = getEnvVar(WEBSITE_SITE_NAME);
        // TODO is the best way to identify running as Azure Functions worker?
        // TODO is this the correct way to match role name from Azure Functions IIS host?
        if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
            // special case for Azure Functions
            return value.toLowerCase(Locale.ENGLISH);
        }
        return value;
    }

    static String overlayWithEnvVar(String name, String defaultValue) {
        String value = getEnvVar(name);
        return value != null ? value : defaultValue;
    }

    static double overlayWithEnvVar(String name, double defaultValue) {
        String value = getEnvVar(name);
        // intentionally allowing NumberFormatException to bubble up as invalid configuration and prevent agent from starting
        return value != null ? Double.parseDouble(value) : defaultValue;
    }

    // never returns empty string (empty string is normalized to null)
    private static String getEnvVar(String name) {
        return trimAndEmptyToNull(System.getenv(name));
    }

    // visible for testing
    static String trimAndEmptyToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isTrimEmpty(String value) {
        return value == null || value.trim().isEmpty();
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

    public static Configuration loadJsonConfigFile(Path configPath) throws IOException{
        if (!Files.exists(configPath)) {
            throw new IllegalStateException("config file does not exist: " + configPath);
        }
        
        BasicFileAttributes attributes = Files.readAttributes(configPath, BasicFileAttributes.class);
        // important to read last modified before reading the file, to prevent possible race condition
        // where file is updated after reading it but before reading last modified, and then since
        // last modified doesn't change after that, the new updated file will not be read afterwards
        long lastModifiedTime = attributes.lastModifiedTime().toMillis();
        try (InputStream in = Files.newInputStream(configPath)) {
            Moshi moshi = MoshiBuilderFactory.createBuilderWithAdaptor();
            JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class).failOnUnknown();
            Buffer buffer = new Buffer();
            buffer.readFrom(in);
            try {
                Configuration configuration = jsonAdapter.fromJson(buffer);
                if (configuration.instrumentationSettings != null) {
                    throw new IllegalStateException("It looks like you are using an old applicationinsights.json file" +
                            " which still has \"instrumentationSettings\", please see the docs for the new format:" +
                            " https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
                }
                configuration.configPath = configPath;
                configuration.lastModifiedTime = lastModifiedTime;
                return configuration;
            } catch(JsonDataException ex) {
                throw new FriendlyException("Application Insights Java agent's configuration file "+configPath.toAbsolutePath().toString()+" has the following issue:\n"+ex.getMessage(),
                        "Learn more about configuration options here: https://go.microsoft.com/fwlink/?linkid=2153358");

            } catch (JsonEncodingException ex) {
                throw new FriendlyException("Application Insights Java agent's configuration file "+configPath.toAbsolutePath().toString()+" has the following syntax issue:\n"+ex.getMessage(),
                        "Learn more about configuration options here: https://go.microsoft.com/fwlink/?linkid=2153358");

            } catch(Exception e) {
                throw new ConfigurationException("Error parsing configuration file: " + configPath.toAbsolutePath().toString(), e);
            }
        }
    }
}
