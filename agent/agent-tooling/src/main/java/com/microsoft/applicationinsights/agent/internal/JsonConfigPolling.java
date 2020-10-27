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
import com.microsoft.applicationinsights.agent.bootstrap.MainEntryPoint;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.Sampling;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JsonConfigPolling implements Runnable {

    private volatile Long lastModifiedTime = MainEntryPoint.getLastModifiedTime();
    private static final Logger logger = LoggerFactory.getLogger(JsonConfigPolling.class);

    public static void pollJsonConfigEveryMinute() {
        Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(JsonConfigPolling.class))
                .scheduleWithFixedDelay(new JsonConfigPolling(), 60, 60, SECONDS);
    }

    @Override public void run() {
        Path path = MainEntryPoint.getConfigPath();
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
                if (!configuration.connectionString.equals(TelemetryConfiguration.getActive().getConnectionString())) {
                    logger.debug("Connection string from the JSON config file is overriding the previously configured connection string.");
                    TelemetryConfiguration.getActive().setConnectionString(configuration.connectionString);
                }

                Sampling sampling = configuration.sampling;
                if (sampling != null && sampling.percentage != Global.getSamplingPercentage()) {
                    logger.debug("Override fixed rate sampling percentage from " + Global.getSamplingPercentage() + " to " + sampling.percentage + " ");
                    Global.setSamplingPercentage(sampling.percentage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Error occurred when polling json config file: " + e.toString());
        }
    }
}
