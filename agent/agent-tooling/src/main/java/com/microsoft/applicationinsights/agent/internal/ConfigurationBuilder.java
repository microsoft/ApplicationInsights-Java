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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okio.Okio;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigurationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);

    private static final String APPLICATIONINSIGHTS_CONFIGURATION_CONTENT = "APPLICATIONINSIGHTS_CONFIGURATION_CONTENT";

    private static final String APPLICATIONINSIGHTS_ROLE_NAME = "APPLICATIONINSIGHTS_ROLE_NAME";
    private static final String APPLICATIONINSIGHTS_ROLE_INSTANCE = "APPLICATIONINSIGHTS_ROLE_INSTANCE";

    private static final String APPLICATIONINSIGHTS_HTTP_PROXY = "APPLICATIONINSIGHTS_HTTP_PROXY";

    private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
    private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";

    private static final Converter<String, String> envVarNameConverter =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE);

    static Configuration create(Path agentJarPath) throws IOException {

        Configuration config = loadConfigurationFile(agentJarPath);

        config.roleName = overlayWithEnvVar(APPLICATIONINSIGHTS_ROLE_NAME, WEBSITE_SITE_NAME, config.roleName);
        config.roleInstance =
                overlayWithEnvVar(APPLICATIONINSIGHTS_ROLE_INSTANCE, WEBSITE_INSTANCE_ID, config.roleInstance);

        config.httpProxy = overlayWithEnvVar(APPLICATIONINSIGHTS_HTTP_PROXY, config.httpProxy);

        return config;
    }

    private static Configuration loadConfigurationFile(Path agentJarPath) throws IOException {

        String configurationContent = System.getenv(APPLICATIONINSIGHTS_CONFIGURATION_CONTENT);
        if (!Strings.isNullOrEmpty(configurationContent)) {
            logger.debug("reading configuration from env var");
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
        String configPathStr = getStringProperty("configurationFile");
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
                try {
                    return jsonAdapter.fromJson(Okio.buffer(Okio.source(in)));
                } catch (Exception e) {
                    throw new ConfigurationException(
                            "Error parsing configuration file: " + configPath.toAbsolutePath().toString(), e);
                }
            }
        } else {
            if (warnIfMissing) {
                logger.warn("could not find configuration file: {}", configPathStr);
            }
            return new Configuration();
        }
    }

    // never returns empty string (empty string is normalized to null)
    @Nullable
    private static String getStringProperty(String propertyName) {
        String propertyValue = System.getenv("APPLICATIONINSIGHTS_" + getEnvVarName(propertyName));
        if (!isNullOrEmpty(propertyValue)) {
            return propertyValue.trim();
        }
        propertyValue = System.getProperty("applicationInsights." + propertyName);
        if (!isNullOrEmpty(propertyValue)) {
            return propertyValue.trim();
        }
        return null;
    }

    @VisibleForTesting
    static String overlayWithEnvVar(String name1, String name2, String defaultValue) {
        String value = getEnv(name1);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }
        value = getEnv(name2);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }
        return defaultValue;
    }

    static String overlayWithEnvVar(String name, String defaultValue) {
        String value = getEnv(name);
        if (!Strings.isNullOrEmpty(value)) {
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

    @VisibleForTesting
    static Map<String, String> overlayWithEnvVar(String name, Map<String, String> defaultValue) {
        String value = System.getenv(name);
        if (!Strings.isNullOrEmpty(value)) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            Map<String, String> stringMap = new HashMap<>();
            Map<String, Object> objectMap;
            try {
                objectMap = adapter.fromJson(value);
            } catch (Exception e) {
                logger.warn("could not parse environment variable {} as json: {}", name, value);
                return defaultValue;
            }
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof String)) {
                    logger.warn("currently only string values are supported in json map from {}: {}", name, value);
                    return defaultValue;
                }
                stringMap.put(entry.getKey(), (String) val);
            }
            return stringMap;
        }
        return defaultValue;
    }

    @VisibleForTesting
    static String getEnvVarName(String propertyName) {
        return envVarNameConverter.convert(propertyName.replace('.', '_'));
    }

    @VisibleForTesting
    static boolean isNullOrEmpty(String str) {
        return Strings.isNullOrEmpty(str) || CharMatcher.is(' ').matchesAllOf(str);
    }

    static class ConfigurationException extends RuntimeException {

        ConfigurationException(String message) {
            super(message);
        }

        ConfigurationException(String message, Exception e) {
            super(message, e);
        }
    }
}
