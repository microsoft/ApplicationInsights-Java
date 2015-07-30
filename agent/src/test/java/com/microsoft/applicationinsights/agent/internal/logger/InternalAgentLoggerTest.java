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

package com.microsoft.applicationinsights.agent.internal.logger;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public final class InternalAgentLoggerTest {
    @Before
    public void preTest() throws NoSuchFieldException, IllegalAccessException {
        Field field = InternalAgentLogger.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(InternalAgentLogger.INSTANCE, false);

        field = InternalAgentLogger.class.getDeclaredField("loggingLevel");
        field.setAccessible(true);
        field.set(InternalAgentLogger.INSTANCE, InternalAgentLogger.LoggingLevel.OFF);
    }

    @Test
    public void testInitializeWithBadValue() {
        InternalAgentLogger.INSTANCE.initialize("asdf");

        assertFalse(InternalAgentLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testInitializeWithEmptyValue() {
        InternalAgentLogger.INSTANCE.initialize("");

        assertTrue(InternalAgentLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testInitializeWithNullyValue() {
        InternalAgentLogger.INSTANCE.initialize(null);

        assertTrue(InternalAgentLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testNotInitialized() {
        assertFalse(InternalAgentLogger.INSTANCE.isTraceEnabled());
    }

    @Test
    public void testInitializedTwice() {
        InternalAgentLogger.INSTANCE.initialize("WARN");
        InternalAgentLogger.INSTANCE.initialize("TRACE");

        assertTrue(InternalAgentLogger.INSTANCE.isWarnEnabled());
        assertTrue(InternalAgentLogger.INSTANCE.isErrorEnabled());
        assertFalse(InternalAgentLogger.INSTANCE.isTraceEnabled());
    }
}
