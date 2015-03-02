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

package com.microsoft.applicationinsights.telemetry;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import static org.junit.Assert.*;

public final class PerformanceCounterTelemetryTest {
    private final static String MOCK_CATEGORY = "Mock_Category";
    private final static String MOCK_COUNTER = "Mock_Counter";
    private final static String MOCK_INSTANCE = "Mock_Instance";
    private final static double MOCK_VALUE = 222.1;

    @Test(expected = IllegalArgumentException.class)
    public void testNullCategoryName() throws IOException {
        new PerformanceCounterTelemetry(null, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCategoryName() throws IOException {
        new PerformanceCounterTelemetry("", MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCounterName() throws IOException {
        new PerformanceCounterTelemetry(MOCK_CATEGORY, null, MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCounterName() throws IOException {
        new PerformanceCounterTelemetry(MOCK_CATEGORY, "", MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test
    public void testStateAfterCtor() throws IOException {
        PerformanceCounterTelemetry telemetry = new PerformanceCounterTelemetry(MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);

        assertEquals(MOCK_CATEGORY, telemetry.getCategoryName());
        assertEquals(MOCK_COUNTER, telemetry.getCounterName());
        assertEquals(MOCK_INSTANCE, telemetry.getInstanceName());
        assertEquals(MOCK_VALUE, telemetry.getValue(), 0.0);
        verifyJson(telemetry, MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test
    public void testCategoryName() throws IOException {
        PerformanceCounterTelemetry telemetry = new PerformanceCounterTelemetry(MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);

        String newValue = MOCK_CATEGORY + "new";
        telemetry.setCategoryName(newValue);
        assertEquals(newValue, telemetry.getCategoryName());
        verifyJson(telemetry, newValue, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test
    public void testCounterName() throws IOException {
        PerformanceCounterTelemetry telemetry = new PerformanceCounterTelemetry(MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);

        String newValue = MOCK_COUNTER + "new";
        telemetry.setCounterName(newValue);
        assertEquals(newValue, telemetry.getCounterName());
        verifyJson(telemetry, MOCK_CATEGORY, newValue, MOCK_INSTANCE, MOCK_VALUE);
    }

    @Test
    public void testInstanceName() throws IOException {
        PerformanceCounterTelemetry telemetry = new PerformanceCounterTelemetry(MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);

        String newValue = MOCK_INSTANCE + "new";
        telemetry.setInstanceName(newValue);
        assertEquals(newValue, telemetry.getInstanceName());
        verifyJson(telemetry, MOCK_CATEGORY, MOCK_COUNTER, newValue, MOCK_VALUE);
    }

    @Test
    public void testValue() throws IOException {
        PerformanceCounterTelemetry telemetry = new PerformanceCounterTelemetry(MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, MOCK_VALUE);

        double newValue = MOCK_VALUE + 1;
        telemetry.setValue(newValue);
        assertEquals(newValue, telemetry.getValue(), 0.0);
        verifyJson(telemetry, MOCK_CATEGORY, MOCK_COUNTER, MOCK_INSTANCE, newValue);
    }

    private static void verifyJson(
            PerformanceCounterTelemetry telemetry,
            String expectedCategory,
            String expectedCounter,
            String expectedInstance,
            double expectedValue) throws IOException {
        telemetry.setTimestamp(new Date());
        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = null;
        jsonWriter = new JsonTelemetryDataSerializer(writer);
        telemetry.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();
        String expectedPerformanceDataPartFormat = "\"baseData\":{\"ver\":\"2\",\"categoryName\":\"%s\",\"counterName\":\"%s\",\"instanceName\":\"%s\",\"value\":\"%.1f\"}}}";
        String expected = String.format(expectedPerformanceDataPartFormat, expectedCategory, expectedCounter, expectedInstance, expectedValue);
        assertTrue(asJson.indexOf(expected) != -1);
    }
}