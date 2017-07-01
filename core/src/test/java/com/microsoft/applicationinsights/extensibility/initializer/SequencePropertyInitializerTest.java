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

package com.microsoft.applicationinsights.extensibility.initializer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class SequencePropertyInitializerTest {
    private static class StubDomainData extends Domain {
        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {

        }
    }

    private static class StubTelemetry extends BaseTelemetry<StubDomainData> {
        public StubTelemetry() {
        }

        public StubTelemetry(String d) {
            initialize(new ConcurrentHashMap<String, String>());
        }

        @Override
        protected void additionalSanitize() {
        }

        @Override
        protected StubDomainData getData() {
            return null;
        }
    }

    @Test
    public void testTelemetryUpdatesAfterCtor() {
        StubTelemetry telemetry = new StubTelemetry();
        assertNull(telemetry.getSequence());

        SequencePropertyInitializer initializer = new SequencePropertyInitializer();
        initializer.initialize(telemetry);

        String sequence = telemetry.getSequence();
        assertNotNull(sequence);
        String[] strings = sequence.split(":");
        assertEquals(strings.length, 2);
        assertEquals(strings[1], "0");
    }

    @Test
    public void testTelemetryNotUpdatesAfterInitialization() {
        StubTelemetry telemetry = new StubTelemetry();

        SequencePropertyInitializer initializer = new SequencePropertyInitializer();
        initializer.initialize(telemetry);

        String sequence = telemetry.getSequence();
        initializer.initialize(telemetry);

        assertEquals(telemetry.getSequence(), sequence);
    }

    @Test
    public void testTelemetryNotUpdatesAfterSet() {
        StubTelemetry telemetry = new StubTelemetry();
        telemetry.setSequence("MOCK");

        SequencePropertyInitializer initializer = new SequencePropertyInitializer();
        initializer.initialize(telemetry);

        assertEquals(telemetry.getSequence(), "MOCK");
    }

    @Test
    public void testTelemetryUpdatesTwoTelemetries() {
        StubTelemetry telemetry1 = new StubTelemetry();
        StubTelemetry telemetry2 = new StubTelemetry();

        SequencePropertyInitializer initializer = new SequencePropertyInitializer();
        initializer.initialize(telemetry1);
        initializer.initialize(telemetry2);

        String sequence1 = telemetry1.getSequence();
        String sequence2 = telemetry2.getSequence();
        assertNotEquals(sequence1, sequence2);

        String[] strings1 = sequence1.split(":");
        String[] strings2 = sequence2.split(":");

        assertEquals(strings1[0], strings2[0]);
        assertEquals(strings1[1], "0");
        assertEquals(strings2[1], "1");
    }

    @Test
    public void testDifferentTelemetries() {
        SequencePropertyInitializer initializer1 = new SequencePropertyInitializer();
        SequencePropertyInitializer initializer2 = new SequencePropertyInitializer();

        StubTelemetry telemetry1 = new StubTelemetry();
        StubTelemetry telemetry2 = new StubTelemetry();

        initializer1.initialize(telemetry1);
        initializer2.initialize(telemetry2);

        assertNotEquals(telemetry1.getSequence(), telemetry2.getSequence());
    }
}