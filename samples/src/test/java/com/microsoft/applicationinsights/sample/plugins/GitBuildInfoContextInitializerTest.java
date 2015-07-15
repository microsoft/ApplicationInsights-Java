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

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.sample.plugins.GitBuildInfoContextInitializerTest;
import org.junit.Test;

import static org.junit.Assert.*;

public final class GitBuildInfoContextInitializerTest {
    private final static String TEST_GIT_BRANCH_VALUE = "features/my-branch";
    private final static String TEST_GIT_COMMIT_VALUE = "commit-value";
    private final static String TEST_GIT_REPO_VALUE = "https://github.com/MyCompany/MyProject.git";
    private final static String TEST_GIT_URL_VALUE = "https://github.com/MyCompany/MyProject/commit/commit-value";

    @Test
    public void loadSourcePropertiesTest() {
        GitBuildInfoContextInitializer initializer = new GitBuildInfoContextInitializer();
        TelemetryContext context = new TelemetryContext();
        initializer.initialize(context);

        verify(context, GitBuildInfoContextInitializer.GIT_REPO_KEY, TEST_GIT_REPO_VALUE);
        verify(context, GitBuildInfoContextInitializer.GIT_COMMIT_KEY, TEST_GIT_COMMIT_VALUE);
        verify(context, GitBuildInfoContextInitializer.GIT_BRANCH_KEY, TEST_GIT_BRANCH_VALUE);
        verify(context, GitBuildInfoContextInitializer.GIT_COMMIT_URL_KEY, TEST_GIT_URL_VALUE);
    }

    private static void verify(TelemetryContext context,
                               String key,
                               String expectedValue) {
        String value = context.getProperties().get(key);
        assertEquals(expectedValue, value);
    }
}