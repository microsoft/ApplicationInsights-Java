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

package com.microsoft.applicationinsights.extensibility.initializer.docker;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.Constants;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.DockerContext;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.DockerContextPoller;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.FileFactory;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

// Created by yonisha on 7/29/2015.
/**
 * 
 *
 * This initializer is required for Docker awareness.
 * The initializer has two goals:
 *  1) Identify Docker context file injected by Application Insight for Docker container and initialize
 *      telemetries with Docker context.
 *  2) Write SDK context file which enables the Application Insights for Docker container to discover containers with
 *      integrated SDK, and stop sending performance counters in that case (SDK provides OOB performance counters).
 */
public class DockerContextInitializer implements TelemetryInitializer {

    private FileFactory fileFactory;
    private DockerContextPoller dockerContextPoller;
    private boolean sdkInfoFileWritten = false;

    protected DockerContextInitializer(FileFactory fileFactory, DockerContextPoller dockerContextPoller) {
        this.fileFactory = fileFactory;
        this.dockerContextPoller = dockerContextPoller;
        this.dockerContextPoller.start();
    }

    /**
     * Constructs new @DockerContextInitializer.
     * The constructor start a dedicated thread that periodically checks if a context file exists, and if so, updates
     * the docker context.
     */
    public DockerContextInitializer() {
        this(new FileFactory(), new DockerContextPoller(Constants.AI_SDK_DIRECTORY));
    }

    /**
     * Initialize the given telemetry with the Docker context.
     * @param telemetry A Telemetry to initialize.
     */
    @Override
    public void initialize(Telemetry telemetry) {
        DockerContext dockerContext;

        if (!sdkInfoFileWritten) {
            synchronized (this) {
                if (!sdkInfoFileWritten) {
                    writeSDKInfoFile();
                    sdkInfoFileWritten = true;
                }
            }
        }

        if (dockerContextPoller.isCompleted() && (dockerContext = dockerContextPoller.getDockerContext()) != null) {
            TelemetryContext context = telemetry.getContext();

            // We always set the device ID, since by default it is represented by a GUID inside Docker container.
            String containerName = dockerContext.getProperties().get(Constants.DOCKER_CONTAINER_NAME_PROPERTY_KEY);
            context.getDevice().setId(containerName);

            // If telemetry already initialized with Docker properties, we don't overwrite it.
            if (!LocalStringsUtils.isNullOrEmpty(context.getProperties().get(Constants.DOCKER_HOST_PROPERTY_KEY))) {
                return;
            }

            ConcurrentMap<String, String> properties = context.getProperties();
            properties.putAll(dockerContext.getProperties());
        }
    }

    private void writeSDKInfoFile() {
        String sdkInfoFilePath = String.format("%s/%s", Constants.AI_SDK_DIRECTORY, Constants.AI_SDK_INFO_FILENAME);
        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String sdkInfo = String.format(Constants.AI_SDK_INFO_FILE_CONTENT_TEMPLATE, instrumentationKey);

        try {
            fileFactory.create(sdkInfoFilePath, sdkInfo);
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to write SDK info file for Docker awareness. Error: %s", e.toString());
        }
    }
}
