/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.extensibility.initializer;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by gupele on 7/8/2015.
 */
public final class BuildContextInitializer implements ContextInitializer {

    private final static String BUILD_INFO_FILE_NAME = "source-origin.properties";

    final static String GIT_BRANCH_NAME = "git.branch";
    final static String GIT_COMMIT_NAME = "git.commit";
    final static String GIT_URL_NAME = "git.url";

    final static String BUILD_DEFAULT_VALUE = "unknown";

    private String gitBranch;
    private String gitCommit;
    private String gitUrl;
    private boolean hasBuildData;

    public BuildContextInitializer() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(BUILD_INFO_FILE_NAME);
            if (inputStream == null) {
                hasBuildData = false;
                return;
            }

            Properties buildProperties = new Properties();
            buildProperties.load(inputStream);

            gitUrl = buildProperties.getProperty(GIT_URL_NAME, BUILD_DEFAULT_VALUE);
            gitBranch = buildProperties.getProperty(GIT_BRANCH_NAME, BUILD_DEFAULT_VALUE);
            gitCommit = buildProperties.getProperty(GIT_COMMIT_NAME, BUILD_DEFAULT_VALUE);

            InternalLogger.INSTANCE.trace("Loaded build properties file '%s'", BUILD_INFO_FILE_NAME);

            hasBuildData = true;
        } catch (Throwable t) {
            hasBuildData = false;

            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to load '%s', exception: '%s", BUILD_INFO_FILE_NAME, t.getMessage());
        }
    }

    @Override
    public void initialize(TelemetryContext context) {
        if (!hasBuildData) {
            return;
        }

        context.getProperties().put(GIT_BRANCH_NAME, gitBranch);
        context.getProperties().put(GIT_COMMIT_NAME, gitCommit);
        context.getProperties().put(GIT_URL_NAME, gitUrl);
    }
}
