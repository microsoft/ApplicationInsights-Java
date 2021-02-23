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
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingPercentage;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JsonConfigPolling implements Runnable {

    private final Path path;
    private volatile long lastModifiedTime;
    private volatile double lastReadSamplingPercentage;
    private static final Logger logger = LoggerFactory.getLogger(JsonConfigPolling.class);

    // visible for testing
    JsonConfigPolling(Path path, long lastModifiedTime, double lastReadSamplingPercentage) {
        this.path = path;
        this.lastModifiedTime = lastModifiedTime;
        this.lastReadSamplingPercentage = lastReadSamplingPercentage;
    }

    // passing in lastReadSamplingPercentage instead of using the real samplingPercentage, because the real
    // samplingPercentage is rounded to nearest 100/N, and we want to know specifically when the underlying config value changes
    // which is lastReadSamplingPercentage
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
                // important to overlay env vars here, so that we don't overwrite the value set by env var
                ConfigurationBuilder.overlayEnvVars(configuration);

                if (!configuration.connectionString.equals(TelemetryConfiguration.getActive().getConnectionString())) {
                    logger.debug("Connection string from the JSON config file is overriding the previously configured connection string.");
                    TelemetryConfiguration.getActive().setConnectionString(configuration.connectionString);
                }

                if (configuration.sampling.percentage != lastReadSamplingPercentage) {
                    logger.debug("Updating sampling percentage from {} to {}", lastReadSamplingPercentage, configuration.sampling.percentage);
                    double roundedSamplingPercentage = SamplingPercentage.roundToNearest(configuration.sampling.percentage);
                    DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(roundedSamplingPercentage));
                    Global.setSamplingPercentage(roundedSamplingPercentage);
                    lastReadSamplingPercentage = configuration.sampling.percentage;
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred when polling json config file: {}", e.getMessage(), e);
        }
    }
}
