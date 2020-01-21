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

package com.microsoft.applicationinsights.log4j.v1_2;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.google.common.collect.Maps;
import com.microsoft.applicationinsights.internal.shared.LogChannelMockVerifier;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ApplicationInsightsAppenderTests {

    // region Consts

    private static final String TestInstrumentationKey = "c9341531-05ac-4d8c-972e-36e97601d5ff";

    // endregion Consts

    // region Initialization & cleanup

    @Before
    public void setup() {
        setMockTelemetryChannelToAIAppender();
        Assert.assertTrue("MockTelemetryChannel not clear....", LogChannelMockVerifier.INSTANCE.getTelemetryCollection().isEmpty());
    }

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("Failure detected. Printing found telemetry:");
            List<Telemetry> telemetries = LogChannelMockVerifier.INSTANCE.getTelemetryCollection();
            for (Telemetry t : telemetries) {
                String tname = t.getClass().getSimpleName();
                String tinfo = tname;
                if (t instanceof TraceTelemetry) {
                    TraceTelemetry trct = (TraceTelemetry)t;
                    tinfo = String.format("%s{message='%s'}", tname, trct.getMessage());
                } else if (t instanceof ExceptionTelemetry) {
                    ExceptionTelemetry ext = (ExceptionTelemetry)t;
                    tinfo = String.format("%s{%s[message='%s']}", tname, ext.getException().getClass().getSimpleName(), ext.getException().getMessage());
                }
                System.out.println(" - "+tinfo);
            }
            System.out.printf("%d total telemetries collected.%n", telemetries.size());
        }
    };

    // endregion Initialization & cleanup

    // region Tests

    @Test
    public void testInstrumentationKeyIsLoadedFromConfiguration() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();

        String configurationKey = appender.getTelemetryClientProxy().getTelemetryClient().getContext().getInstrumentationKey();
        Assert.assertEquals(TestInstrumentationKey, configurationKey);
    }

    @Test
    public void testAppenderSendsGivenEvent() throws Exception {
        Logger logger = LogManager.getRootLogger();
        logger.trace("New event!");
        TimeUnit.SECONDS.sleep(1);

        Assert.assertEquals(1, LogChannelMockVerifier.INSTANCE.getTelemetryCollection().size());
    }

    @Test
    public void testLoggerMessageIsRetainedWhenReportingException() throws Exception {
        Logger logger = LogManager.getRootLogger();
        logger.error("This is an exception", new Exception("Fake Exception"));
        TimeUnit.SECONDS.sleep(1);

        Assert.assertEquals(1, LogChannelMockVerifier.INSTANCE.getTelemetryCollection().size());
        Assert.assertTrue(LogChannelMockVerifier.INSTANCE.getTelemetryCollection().get(0).getProperties().containsKey("Logger Message"));
        Assert.assertTrue(LogChannelMockVerifier.INSTANCE.getTelemetryCollection().get(0).getProperties().get("Logger Message").equals("This is an exception"));
    }

    // endregion Tests

    // region Private methods

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = LogManager.getRootLogger();
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender)logger.getAppender("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();
        appender.activateOptions();
        LogChannelMockVerifier.INSTANCE.getTelemetryCollection().clear();
    }

    // endregion Private methods
}