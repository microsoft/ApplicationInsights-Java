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

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.DockerContext;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.DockerContextPoller;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by yonisha on 7/29/2015.
 */
public class DockerContextInitializer implements TelemetryInitializer {

    private DockerContextPoller dockerContextPoller;

    protected DockerContextInitializer(DockerContextPoller dockerContextPoller) {
        this.dockerContextPoller = dockerContextPoller;
    }

    /**
     * Constructs new @DockerContextInitializer.
     * The constructor start a dedicated thread that periodically checks if a context file exists, and if so, updates
     * the docker context.
     */
    public DockerContextInitializer() {

        // TODO: inject SDK-aware file into docker directory.

        dockerContextPoller = new DockerContextPoller();
        dockerContextPoller.start();
    }

    /**
     * Initialize the given telemetry with the Docker context.
     * @param telemetry A Telemetry to initialize.
     */
    @Override
    public void initialize(Telemetry telemetry) {
        DockerContext dockerContext;
        if (dockerContextPoller.isCompleted() && (dockerContext = dockerContextPoller.getDockerContext()) != null) {
            TelemetryContext context = telemetry.getContext();

            context.getDevice().setId(dockerContext.getHostName());

            ConcurrentMap<String, String> properties = context.getProperties();
            properties.putAll(dockerContext.getProperties());
        }
    }
}
