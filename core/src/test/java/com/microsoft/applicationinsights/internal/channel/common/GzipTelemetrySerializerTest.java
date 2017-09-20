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

package com.microsoft.applicationinsights.internal.channel.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.junit.Test;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;

import com.google.common.base.Optional;
import com.google.gson.Gson;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public final class GzipTelemetrySerializerTest {
    private static class StubTelemetry implements Telemetry {
        private final String telemetryName;

        private final TelemetryContext context = new TelemetryContext();

        private final HashMap<String, String> properties;

        public StubTelemetry(String telemetryName, HashMap<String, String> properties) {
            this.telemetryName = telemetryName;
            this.properties = properties;
        }

        @Override
        public Date getTimestamp() {
            return null;
        }

        @Override
        public String getSequence() {
            return null;
        }

        @Override
        public void setSequence(String sequence) {
        }

        @Override
        public void setTimestamp(Date date) {
        }

        @Override
        public TelemetryContext getContext() {
            return context;
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public void sanitize() {
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof StubTelemetry)) {
                return false;
            }

            StubTelemetry that = (StubTelemetry)other;
            return
                    this.telemetryName.equals(that.getTelemetryName()) &&
                    this.getProperties().equals(that.getProperties());
        }

        public String getTelemetryName() {
            return telemetryName;
        }

        @Override
        public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
            writer.write("ver", 1);
            writer.write("telemetryName", telemetryName, 150, true);
            writer.write("properties", this.getProperties());
        }

        @Override
        public void reset() {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNull() throws Exception {
        GzipTelemetrySerializer tested = new GzipTelemetrySerializer();
        tested.serialize(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoData() throws Exception {
        GzipTelemetrySerializer tested = new GzipTelemetrySerializer();
        tested.serialize(new ArrayList<String>());
    }

    @Test
    public void testSerializeOfOne() throws Exception {
        testSerialization(1);
    }

    @Test
    public void testSerializeOfTwo() throws Exception {
        testSerialization(2);
    }

    @Test
    public void testSerializeOfTen() throws Exception {
        testSerialization(10);
    }

    private void testSerialization(int amount) throws Exception {
        GzipTelemetrySerializer tested = new GzipTelemetrySerializer();

        List<Telemetry> telemetries = new ArrayList<Telemetry>(amount);
        List<String> telemetriesSerialized = new ArrayList<String>(amount);

        HashMap<String, StubTelemetry> expected = new HashMap<String, StubTelemetry>();

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        for (int i = 0; i < amount; ++i) {
            StubTelemetry stubTelemetry = createStubTelemetry(String.valueOf(i));
            telemetries.add(stubTelemetry);

            stubTelemetry.serialize(jsonWriter);
            jsonWriter.close();
            String asJson = writer.toString();

            telemetriesSerialized.add(asJson);
            writer.getBuffer().setLength(0);
            jsonWriter.reset(writer);

            expected.put(stubTelemetry.getTelemetryName(), stubTelemetry);
        }

        Optional<Transmission> result = tested.serialize(telemetriesSerialized);

        assertNotNull(result);

        Transmission serializationData = result.get();
        assertNotNull(serializationData);
        assertNotNull(serializationData.getContent());
        assertEquals(serializationData.getWebContentType(), "application/x-json-stream");
        assertEquals(serializationData.getWebContentEncodingType(), "gzip");

        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(serializationData.getContent()));
        try {
            ByteArrayOutputStream contents = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len, totalLen = 0;

            while ((len = gis.read(buf)) > 0) {
                contents.write(buf, 0, len);
                totalLen += len;
            }

            String value = new String(contents.toByteArray());
            String[] stubStrings = value.split(System.getProperty("line.separator"));

            assertEquals(stubStrings.length, amount);

            Gson gson = new Gson();
            for (String stubString : stubStrings) {
                StubTelemetry stubTelemetry = gson.fromJson(stubString, StubTelemetry.class);

                StubTelemetry expectedStub = expected.get(stubTelemetry.getTelemetryName());
                assertNotNull(expectedStub);
                assertEquals(stubTelemetry, expectedStub);
            }

        } catch (IOException e) {
            assertTrue(false);
        } finally {
            gis.close();
        }
    }

    private StubTelemetry createStubTelemetry(String index) {
        HashMap<String, String> hash1 = new HashMap<String, String>();
        hash1.put("mock" + index + "_1", "value1" + index + "_1");
        hash1.put("mock" + index + "_2", "value1" + index + "_2");
        StubTelemetry stubTelemetry = new StubTelemetry("stub" + index, hash1);

        return stubTelemetry;
    }
}