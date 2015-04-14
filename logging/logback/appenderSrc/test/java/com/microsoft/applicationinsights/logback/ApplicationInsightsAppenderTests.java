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

package com.microsoft.applicationinsights.logback;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import com.microsoft.applicationinsights.internal.shared.LogChannelMockVerifier;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;

public class ApplicationInsightsAppenderTests {

    // region Consts

    private static final String TestInstrumentationKey = "c9341531-05ac-4d8c-972e-36e97601d5ff";

    // endregion Consts

    // region Initialization & cleanup

    @Before
    public void setup() {
        setMockTelemetryChannelToAIAppender();
    }

    // endregion Initialization & cleanup

    // region Tests

    @Test
    public void testInstrumentationKeyIsLoadedFromConfiguration() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();

        String configurationKey = appender.getTelemetryClientProxy().getTelemetryClient().getContext().getInstrumentationKey();
        Assert.assertEquals(TestInstrumentationKey, configurationKey);
    }

    @Test
    public void testAppenderSendsGivenEvent() {
        Logger logger = (Logger) LoggerFactory.getLogger("root");
        logger.trace("Hello");

        Assert.assertEquals(1, LogChannelMockVerifier.INSTANCE.getTelemetryCollection().size());
    }

    // endregion Tests

    // region Private methods

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger("root");
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender) logger.getAppender("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        LogChannelMockVerifier.INSTANCE.getTelemetryCollection().clear();
    }

    // endregion Private methods
}