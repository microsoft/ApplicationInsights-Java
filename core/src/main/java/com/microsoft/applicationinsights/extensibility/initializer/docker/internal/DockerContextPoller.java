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

package com.microsoft.applicationinsights.extensibility.initializer.docker.internal;

import java.io.File;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * Created by yonisha on 7/29/2015.
 */
public class DockerContextPoller extends Thread {
    private static final String CONTEXT_FILE_EXPECTED_PATH = "/usr/appinsights/docker/docker.info";
    private File contextFile;
    private DockerContextFactory dockerContextFactory;
    private volatile DockerContext dockerContext;
    protected int THREAD_POLLING_INTERVAL_MS = 2 * 1000; // 2 Seconds.

    protected DockerContextPoller(File contextFile, DockerContextFactory dockerContextFactory) {
        this.contextFile = contextFile;
        this.dockerContextFactory = dockerContextFactory;
    }

    public DockerContextPoller() {
        this.setDaemon(true);
        this.contextFile = new File(CONTEXT_FILE_EXPECTED_PATH);
        this.dockerContextFactory = new DockerContextFactory();
    }

    @Override
    public void run() {
        boolean fileExists = false;
        while (!fileExists){
            fileExists = contextFile.exists();

            if (!fileExists) {
                try {
                    Thread.sleep(THREAD_POLLING_INTERVAL_MS);

                    continue;
                } catch (InterruptedException e) {
                }
            }
        }

        try {
            this.dockerContext = this.dockerContextFactory.createDockerContext(this.contextFile);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Docker context file has been found but failed to be parsed.");
        }
    }

    public boolean isCompleted() {
        return !this.isAlive();
    }

    public DockerContext getDockerContext() {
        return this.dockerContext;
    }
}