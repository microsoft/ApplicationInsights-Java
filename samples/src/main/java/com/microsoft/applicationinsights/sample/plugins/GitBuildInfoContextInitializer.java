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

package com.microsoft.applicationinsights.sample.plugins;

import java.io.InputStream;
import java.util.Properties;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;

/**
 * An initializer to fetch Git data from a properties file
 * Note: the file with its full package name should appear in the 'ApplicationInsights.xml':
 *
 * <ContextInitializers>
 *   <Add type="com.microsoft.applicationinsights.sample.plugins.GitBuildInfoContextInitializer" />
 * </ContextInitializers>
 */
public final class GitBuildInfoContextInitializer implements ContextInitializer {
    private final static String BUILD_INFO_FILE_NAME = "source-origin.properties";

    final static String GIT_REPO_KEY = "git.repo";
    final static String GIT_BRANCH_KEY = "git.branch";
    final static String GIT_COMMIT_KEY = "git.commit";
    final static String GIT_COMMIT_URL_KEY = "git.commit.url";

    final static String GIT_REPO_SUFFIX = ".git";

    final static String UNKNOWN_SOURCE_VALUE = "unknown";

    private String gitBranch;
    private String gitCommit;
    private String gitRepo;
    private String gitCommitUrl;
    private boolean hasBuildData;

    public GitBuildInfoContextInitializer() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(BUILD_INFO_FILE_NAME);
            if (inputStream == null) {
                hasBuildData = false;
                return;
            }

            Properties buildProperties = new Properties();
            buildProperties.load(inputStream);

            gitRepo = buildProperties.getProperty(GIT_REPO_KEY, UNKNOWN_SOURCE_VALUE);
            gitBranch = buildProperties.getProperty(GIT_BRANCH_KEY, UNKNOWN_SOURCE_VALUE);
            gitCommit = buildProperties.getProperty(GIT_COMMIT_KEY, UNKNOWN_SOURCE_VALUE);

            gitCommitUrl = UNKNOWN_SOURCE_VALUE;
            if (!gitRepo.equals(UNKNOWN_SOURCE_VALUE) &&
                !gitCommit.equals(UNKNOWN_SOURCE_VALUE)) {

                int index = gitRepo.indexOf(GIT_REPO_SUFFIX);
                if (index != -1) {
                    gitCommitUrl = gitRepo.substring(0, index) + '/' + gitCommit;
                }
            }

            hasBuildData = true;
        } catch (Throwable t) {
            hasBuildData = false;
        }
    }

    @Override
    public void initialize(TelemetryContext context) {
        if (!hasBuildData) {
            return;
        }

        context.getProperties().put(GIT_BRANCH_KEY, gitBranch);
        context.getProperties().put(GIT_COMMIT_KEY, gitCommit);
        context.getProperties().put(GIT_REPO_KEY, gitRepo);
        context.getProperties().put(GIT_COMMIT_URL_KEY, gitCommitUrl);
    }
}
