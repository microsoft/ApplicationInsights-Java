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

package com.microsoft.applicationinsights.log4j.v1_2.internal;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ApplicationInsightsLogEventTest {
    @Test
    public void testFatalSeverityLevel() {
        testSeverityLevel(Level.FATAL, SeverityLevel.Critical);
    }

    @Test
    public void testErrorSeverityLevel() {
        testSeverityLevel(Level.ERROR, SeverityLevel.Error);
    }

    @Test
    public void testInfoSeverityLevel() {
        testSeverityLevel(Level.INFO, SeverityLevel.Information);
    }

    @Test
    public void testWarningSeverityLevel() {
        testSeverityLevel(Level.WARN, SeverityLevel.Warning);
    }

    @Test
    public void testDebugSeverityLevel() {
        testSeverityLevel(Level.DEBUG, SeverityLevel.Verbose);
    }

    @Test
    public void testTraceSeverityLevel() {
        testSeverityLevel(Level.TRACE, SeverityLevel.Verbose);
    }

    @Test
    public void testAllSeverityLevel() {
        testSeverityLevel(Level.ALL, SeverityLevel.Verbose);
    }

    private static void testSeverityLevel(Level level, SeverityLevel expected) {
        LoggingEvent loggingEvent = new LoggingEvent("mockClass", Logger.getLogger("com.microsoft.applicationinsights.internal.log4j.v1_2.ApplicationInsightsLogEventTest"), 0L, level, "MockMessage", null);
        ApplicationInsightsLogEvent event = new ApplicationInsightsLogEvent(loggingEvent);

        assertEquals(expected, event.getNormalizedSeverityLevel());
    }
}
