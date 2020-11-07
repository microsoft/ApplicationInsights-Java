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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.Sampling;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingPercentage;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JsonConfigPolling implements Runnable {

    private final Path path;
    private volatile long lastModifiedTime;
    private volatile double lastReadSamplingPercentage;
    private static final Logger logger = LoggerFactory.getLogger(JsonConfigPolling.class);

    private JsonConfigPolling(Path path, long lastModifiedTime, double lastReadSamplingPercentage) {
        this.path = path;
        this.lastModifiedTime = lastModifiedTime;
        this.lastReadSamplingPercentage = lastReadSamplingPercentage;
    }

    public static void pollJsonConfigEveryMinute(Path path, long lastModifiedTime, double lastReadSamplingPercentage) {
        Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(JsonConfigPolling.class))
                .scheduleWithFixedDelay(new JsonConfigPolling(path, lastModifiedTime, lastReadSamplingPercentage), 60, 60, SECONDS);
    }

    @Override
    public void run() {
        if (path == null) {
            logger.warn("JSON config path is null.");
            return;
        }

        if (!Files.exists(path)) {
            logger.warn(path + " doesn't exist.");
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime fileTime = attributes.lastModifiedTime();
            if (lastModifiedTime != fileTime.toMillis()) {
                lastModifiedTime = fileTime.toMillis();
                Configuration configuration = ConfigurationBuilder.loadJsonConfigFile(path);

                // TODO only want to update connectionString with value from configuration file if original value
                //  is from configuration file (not if original value is from APPLICATIONINSIGHTS_CONNECTION_STRING env var)
                if (!configuration.connectionString.equals(TelemetryConfiguration.getActive().getConnectionString())) {
                    logger.debug("Connection string from the JSON config file is overriding the previously configured connection string.");
                    TelemetryConfiguration.getActive().setConnectionString(configuration.connectionString);
                }

                // TODO only want to update sampling percentage with value from configuration file if original value
                //  is from configuration file (not if original value is from APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE env var)
                if (configuration.sampling.percentage != lastReadSamplingPercentage) {
                    logger.debug("Updating sampling percentage from {} to {}", lastReadSamplingPercentage, configuration.sampling.percentage);
                    double roundedSamplingPercentage = SamplingPercentage.roundToNearest(configuration.sampling.percentage);
                    OpenTelemetrySdk.getGlobalTracerManagement().updateActiveTraceConfig(
                            OpenTelemetrySdk.getGlobalTracerManagement().getActiveTraceConfig().toBuilder()
                                    .setSampler(Samplers.getSampler(roundedSamplingPercentage))
                                    .build());
                    Global.setSamplingPercentage(roundedSamplingPercentage);
                    lastReadSamplingPercentage = configuration.sampling.percentage;
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred when polling json config file: {}", e.getMessage(), e);
        }
    }
}
