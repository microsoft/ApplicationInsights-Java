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

package com.microsoft.applicationinsights.web.spring;

import com.microsoft.applicationinsights.test.framework.utils.PropertiesUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by amnonsh on 5/28/2015.
 */
public class TestSettings {
    private Properties testProps;

    private final String SETTINGS_RESOURCE_NAME  = "testSettings.properties";

    public static final String KEY_MAX_WAIT_TIME       = "maxWaitTime";
    public static final String KEY_POLLING_INTERVAL    = "keyPollingInterval";
    public static final String KEY_MESSAGE_BATCH_SIZE  = "keyMessageBatchSize";

    public TestSettings() throws IOException {
        testProps = PropertiesUtils.loadPropertiesFromResource(SETTINGS_RESOURCE_NAME);
    }

    public Integer getMaxWaitTime() {
        return Integer.parseInt(testProps.getProperty(KEY_MAX_WAIT_TIME));
    }

    public Integer getPollingInterval() {
        return Integer.parseInt(testProps.getProperty(KEY_POLLING_INTERVAL));
    }

    public Integer getMessageBatchSize() {
        return Integer.parseInt(testProps.getProperty(KEY_MESSAGE_BATCH_SIZE));
    }
}