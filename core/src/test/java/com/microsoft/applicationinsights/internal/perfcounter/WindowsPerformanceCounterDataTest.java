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

package com.microsoft.applicationinsights.internal.perfcounter;

import org.junit.Test;

import static org.junit.Assert.*;

public final class WindowsPerformanceCounterDataTest {
    private static final String MOCK_CATEGORY = "category";
    private static final String MOCK_COUNTER = "counter";
    private static final String MOCK_INSTANCE = "instance";
    private static final String MOCK_DISPLAY = "display";

    @Test
    public void testAllAreOk() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);

        assertEquals(data.categoryName, MOCK_CATEGORY);
        assertEquals(data.counterName, MOCK_COUNTER);
        assertEquals(data.instanceName, MOCK_INSTANCE);
        assertEquals(data.displayName, MOCK_DISPLAY);
    }

    @Test(expected = Exception.class)
    public void testInstanceNameIsSelfProcess() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(JniPCConnector.PROCESS_SELF_INSTANCE_NAME).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCategoryName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(null).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCategoryName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName("").
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCounterName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(null).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCounterName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName("").
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCDisplayName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyDisplayName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName("");
    }
}