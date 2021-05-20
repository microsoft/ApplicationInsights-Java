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

package com.microsoft.applicationinsights.telemetry;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Charsets;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import org.junit.*;

import static org.junit.Assert.*;

public final class BaseTelemetryTest {
    private static class StubDomainData extends Domain {
        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) {

        }
    }

    private static class StubTelemetry extends BaseTelemetry<StubDomainData> {

        private static final String ENVELOPE_NAME = "Stub";

        private static final String BASE_TYPE = "StubData";

        public StubTelemetry() {
        }

        public StubTelemetry(@SuppressWarnings("unused") String ignored) {
            initialize(new ConcurrentHashMap<>());
        }

        @Override
        protected StubDomainData getData() {
            return null;
        }

        @Override
        public String getEnvelopName() {
            return ENVELOPE_NAME;
        }

        @Override
        public String getBaseTypeName() {
            return BASE_TYPE;
        }
    }

    @Test
    public void testCtor() {
        StubTelemetry telemetry = new StubTelemetry();

        assertNull(telemetry.getContext());
        assertNull(telemetry.getTimestamp());
    }

    @Test
    public void testCtorWithInitialize() {
        StubTelemetry telemetry = new StubTelemetry("1");

        assertNotNull(telemetry.getContext());
        assertTrue(telemetry.getContext().getProperties().isEmpty());
        assertTrue(telemetry.getContext().getTags().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertNull(telemetry.getTimestamp());
    }

    @Test
    public void testSetTimestamp() {
        StubTelemetry telemetry = new StubTelemetry();

        Date date = new Date();
        telemetry.setTimestamp(date);

        assertEquals(telemetry.getTimestamp(), date);
    }


    @Test
    public void testTelemetryNameWithIkey() throws IOException{
        StubTelemetry telemetry = new StubTelemetry("Test Base Telemetry");
        telemetry.getContext().setInstrumentationKey("AIF-00000000-1111-2222-3333-000000000000");
        telemetry.setTimestamp(new Date());

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        telemetry.serialize(jsonWriter);
        jsonWriter.close();
        writer.close();
        String asJson = new String(buffer.readByteArray(), Charsets.UTF_8);

        int index = asJson.indexOf("\"name\":\"Stub\"");
        assertTrue(index != -1);
    }

    @Test
    public void testTelemetryNameWithIkey_SpecialChar() throws IOException{
        StubTelemetry telemetry = new StubTelemetry("Test Base Telemetry");
        telemetry.getContext().setInstrumentationKey("--. .--");
        telemetry.setTimestamp(new Date());

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        telemetry.serialize(jsonWriter);
        jsonWriter.close();
        writer.close();
        String asJson = new String(buffer.readByteArray(), Charsets.UTF_8);

        int index = asJson.indexOf("\"name\":\"Stub\"");
        assertTrue(index != -1);
    }

    @Test
    public void testTelemetryNameWithIkey_Empty() throws IOException{
        StubTelemetry telemetry = new StubTelemetry("Test Base Telemetry");
        telemetry.setTimestamp(new Date());

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        telemetry.serialize(jsonWriter);
        jsonWriter.close();
        writer.close();
        String asJson = new String(buffer.readByteArray(), Charsets.UTF_8);

        int index = asJson.indexOf("\"name\":\"Stub\"");
        assertTrue(index != -1);
    }


}