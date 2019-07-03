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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Converter;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okio.Okio;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigurationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);

    private static final Converter<String, String> envVarNameConverter =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE);

    static Configuration create(Path agentJarPath) throws IOException {

        Configuration config = loadConfigurationFile(agentJarPath);

        String connectionString = getStringProperty("connectionString");
        if (connectionString != null) {
            config.connectionString = connectionString;
        }

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

    @VisibleForTesting
    static String getEnvVarName(String propertyName) {
        return envVarNameConverter.convert(propertyName.replace('.', '_'));
    }

    @VisibleForTesting
    static boolean isNullOrEmpty(String str) {
        return Strings.isNullOrEmpty(str) || CharMatcher.is(' ').matchesAllOf(str);
    }
}
