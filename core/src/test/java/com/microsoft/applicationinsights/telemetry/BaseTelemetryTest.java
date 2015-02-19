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

import com.microsoft.applicationinsights.internal.schemav2.SendableData;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class BaseTelemetryTest {
    private static class StubSendableData implements SendableData {
        @Override
        public String getEnvelopName() {
            return null;
        }

        @Override
        public String getBaseTypeName() {
            return null;
        }

        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {

        }
    }

    private static class StubTelemetry extends BaseTelemetry<StubSendableData> {
        public int numberOfCallsToAdditionalSanitize;

        public StubTelemetry() {
        }

        public StubTelemetry(String d) {
            initialize(new ConcurrentHashMap<String, String>());
        }

        @Override
        protected void additionalSanitize() {
            ++numberOfCallsToAdditionalSanitize;
        }

        @Override
        protected StubSendableData getData() {
            return null;
        }
    }

    @Test
    public void testCtor() {
        StubTelemetry telemetry = new StubTelemetry();

        assertNull(telemetry.getContext());
        assertNull(telemetry.getTimestamp());
        assertNull(telemetry.getSequence());
    }

    @Test
    public void testCtorWithInitialize() {
        StubTelemetry telemetry = new StubTelemetry("1");

        assertNotNull(telemetry.getContext());
        assertTrue(telemetry.getContext().getProperties().isEmpty());
        assertTrue(telemetry.getContext().getTags().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertNull(telemetry.getTimestamp());
        assertNull(telemetry.getSequence());
    }

    @Test
    public void testSetSequence() {
        StubTelemetry telemetry = new StubTelemetry();

        String mockSequence = "MockSequence";
        telemetry.setSequence(mockSequence);

        assertEquals(mockSequence, telemetry.getSequence());
    }

    @Test
    public void testSetTimestamp() {
        StubTelemetry telemetry = new StubTelemetry();

        Date date = new Date();
        telemetry.setTimestamp(date);

        assertEquals(telemetry.getTimestamp(), date);
    }

    @Test
    public void testSanitize() {
        StubTelemetry telemetry = new StubTelemetry("1");

        telemetry.sanitize();

        assertEquals(telemetry.numberOfCallsToAdditionalSanitize, 1);
    }
}