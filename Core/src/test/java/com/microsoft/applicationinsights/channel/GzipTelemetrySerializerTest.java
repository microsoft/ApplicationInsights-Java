package com.microsoft.applicationinsights.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.microsoft.applicationinsights.util.StringUtil;
import org.junit.Test;

import com.microsoft.applicationinsights.datacontracts.JsonWriter;
import com.microsoft.applicationinsights.datacontracts.TelemetryContext;

import com.google.common.base.Optional;
import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GzipTelemetrySerializerTest {
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
        public void serialize(JsonWriter writer) throws IOException {
            writer.writeStartObject();

            writer.writeProperty("ver", 1);
            writer.writeProperty("telemetryName", telemetryName);

            writer.writeProperty("properties", this.getProperties());

            writer.writeEndObject();
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
    }

    @Test(expected = NullPointerException.class)
    public void testNull() throws Exception {
        GzipTelemetrySerializer tested = new GzipTelemetrySerializer();
        tested.serialize(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoData() throws Exception {
        GzipTelemetrySerializer tested = new GzipTelemetrySerializer();
        tested.serialize(new ArrayList<Telemetry>());
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

        HashMap<String, StubTelemetry> expected = new HashMap<String, StubTelemetry>();

        for (int i = 0; i < amount; ++i) {
            StubTelemetry stubTelemetry = createStubTelemetry(String.valueOf(i));
            telemetries.add(stubTelemetry);

            expected.put(stubTelemetry.getTelemetryName(), stubTelemetry);
        }

        Optional<Transmission> result = tested.serialize(telemetries);

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