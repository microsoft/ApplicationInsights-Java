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
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.Strings;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Okio;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigurationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);

    private static final String APPLICATIONINSIGHTS_ROLE_NAME = "APPLICATIONINSIGHTS_ROLE_NAME";
    private static final String APPLICATIONINSIGHTS_ROLE_INSTANCE = "APPLICATIONINSIGHTS_ROLE_INSTANCE";
    private static final String APPLICATIONINSIGHTS_TELEMETRY_CONTEXT = "APPLICATIONINSIGHTS_TELEMETRY_CONTEXT";

    private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
    private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";

    private static final Converter<String, String> envVarNameConverter =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE);

    static Configuration create(Path agentJarPath) throws IOException {

        Configuration config = loadConfigurationFile(agentJarPath);

        String connectionString = getStringProperty("connectionString");
        if (connectionString != null) {
            config.connectionString = connectionString;
        }
        config.roleName = overlayWithEnvVar(APPLICATIONINSIGHTS_ROLE_NAME, WEBSITE_SITE_NAME, config.roleName);
        config.roleInstance =
                overlayWithEnvVar(APPLICATIONINSIGHTS_ROLE_INSTANCE, WEBSITE_INSTANCE_ID, config.roleInstance);
        config.telemetryContext = overlayWithEnvVar(APPLICATIONINSIGHTS_TELEMETRY_CONTEXT, config.telemetryContext);

        return config;
    }

    private static Configuration loadConfigurationFile(Path agentJarPath) throws IOException {

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
                return jsonAdapter.fromJson(Okio.buffer(Okio.source(in)));
            }
        } else {
            if (warnIfMissing) {
                logger.warn("could not find configuration file: {}", configPathStr);
            }
            return new Configuration();
        }
    }

    // never returns empty string (empty string is normalized to null)
    private static @Nullable String getStringProperty(String propertyName) {
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

    private static String overlayWithEnvVar(String name1, String name2, String defaultValue) {
        String value = System.getenv(name1);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }
        value = System.getenv(name2);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }
        return defaultValue;
    }

    private static Map<String, String> overlayWithEnvVar(String name, Map<String, String> defaultValue) {
        String value = System.getenv(name);
        if (!Strings.isNullOrEmpty(value)) {
            Moshi moshi = new Moshi.Builder().build();
            Type type = Types.newParameterizedType(Map.class, String.class, String.class);
            JsonAdapter<Map<String, String>> adapter = moshi.adapter(type);
            try {
                return adapter.fromJson(value);
            } catch (Exception e) {
                logger.warn("could not parse environment variable {} as json: {}", name, value);
            }
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
}
