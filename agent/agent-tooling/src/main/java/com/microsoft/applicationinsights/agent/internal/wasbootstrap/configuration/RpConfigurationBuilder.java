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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

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

public class RpConfigurationBuilder {

    public static RpConfiguration create(Path agentJarPath) throws IOException {
        Path configPath = agentJarPath.resolveSibling("applicationinsights-rp.json");
        if (Files.exists(configPath)) {
            return loadJsonConfigFile(configPath);
        }
        return null;
    }

    public static RpConfiguration loadJsonConfigFile(Path configPath) throws IOException{
        if (!Files.exists(configPath)) {
            throw new IllegalStateException("rp config file does not exist: " + configPath);
        }

        // For container environment, attribute may get the last modified time of symbol link instead of the real file.
        // Of course the last modified time of symbol link will be unchanged in most cases and then it will fail to
        // polling the configurations.
        // See https://github.com/kubernetes/kubernetes/issues/24215.
        BasicFileAttributes attributes = Files.readAttributes(configPath.toRealPath(), BasicFileAttributes.class);

        // important to read last modified before reading the file, to prevent possible race condition
        // where file is updated after reading it but before reading last modified, and then since
        // last modified doesn't change after that, the new updated file will not be read afterwards
        long lastModifiedTime = attributes.lastModifiedTime().toMillis();
        RpConfiguration configuration = getConfigurationFromConfigFile(configPath);
        configuration.configPath = configPath;
        configuration.lastModifiedTime = lastModifiedTime;
        return configuration;
    }

    private static RpConfiguration getConfigurationFromConfigFile(Path configPath) throws IOException {
        try (InputStream in = Files.newInputStream(configPath)) {
            Buffer buffer = new Buffer();
            buffer.readFrom(in);
            Moshi moshi = MoshiBuilderFactory.createBuilderWithAdaptor();
            JsonAdapter<RpConfiguration> jsonAdapter = moshi.adapter(RpConfiguration.class).failOnUnknown();
            return jsonAdapter.fromJson(buffer);
        }
    }
}
