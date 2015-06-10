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

package com.microsoft.applicationinsights.internal.logger;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public final class InternalLoggerTest {

    private final static String WRONG_LOGGER_OUTPUT_NAME = "wrong";
    private final static String WRONG_LOGGER_LEVEL_NAME = "wrong";

    @Before
    public void prepare() throws NoSuchFieldException, IllegalAccessException {
        Field field = InternalLogger.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(InternalLogger.INSTANCE, false);

        field = InternalLogger.class.getDeclaredField("loggingLevel");
        field.setAccessible(true);
        field.set(InternalLogger.INSTANCE, InternalLogger.LoggingLevel.OFF);

        field = InternalLogger.class.getDeclaredField("loggerOutput");
        field.setAccessible(true);
        field.set(InternalLogger.INSTANCE, null);
    }

    @Test
    public void testNoLoggerLevelData() {
        final Map<String, String> loggerData = new HashMap<String, String>();
        InternalLogger.INSTANCE.initialize(InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

        assertTrue(InternalLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testWrongLoggerLevelName() {
        final Map<String, String> loggerData = new HashMap<String, String>();
        loggerData.put("Level", WRONG_LOGGER_LEVEL_NAME);
        InternalLogger.INSTANCE.initialize(InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

        assertFalse(InternalLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testWrongLoggerOutputName() {
        final Map<String, String> loggerData = new HashMap<String, String>();
        loggerData.put("Level", InternalLogger.LoggingLevel.TRACE.toString());
        InternalLogger.INSTANCE.initialize(WRONG_LOGGER_OUTPUT_NAME, loggerData);

        assertFalse(InternalLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testInitializeTwice() {
        final Map<String, String> loggerData = new HashMap<String, String>();
        loggerData.put("Level", InternalLogger.LoggingLevel.ERROR.toString());
        InternalLogger.INSTANCE.initialize(InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

        loggerData.put("Level", InternalLogger.LoggingLevel.WARN.toString());
        InternalLogger.INSTANCE.initialize(InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

        assertTrue(InternalLogger.INSTANCE.isErrorEnabled());
        assertFalse(InternalLogger.INSTANCE.isWarnEnabled());
    }

    @Test
    public void testLogAlwaysWithLoggerLevelOff() {
        final Map<String, String> loggerData = new HashMap<String, String>();
        loggerData.put("Level", InternalLogger.LoggingLevel.OFF.toString());
        InternalLogger.INSTANCE.initialize(InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);
        InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "hey");
    }
}