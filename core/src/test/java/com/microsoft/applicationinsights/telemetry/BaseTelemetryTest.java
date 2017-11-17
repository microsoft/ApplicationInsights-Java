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

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.SessionStateData;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class BaseTelemetryTest {
    private static class StubDomainData extends Domain {
        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {

        }
    }

    private static class StubTelemetry extends BaseTelemetry<StubDomainData> {
        
        private static final String ENVELOPE_NAME = "Stub";

        private static final String BASE_TYPE = "StubData";
    	
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
    public void testTelemetryNameWithIkey() throws IOException{
    	StubTelemetry telemetry = new StubTelemetry("Test Base Telemetry");
    	telemetry.getContext().setInstrumentationKey("AIF-00000000-1111-2222-3333-000000000000");
    	telemetry.setTimestamp(new Date());
    	
        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        telemetry.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        int index = asJson.indexOf("\"name\":\"Microsoft.ApplicationInsights.aif00000000111122223333000000000000.Stub\"");
        assertTrue(index != -1);
    }
    
    
    @Test
    public void testTelemetryNameWithIkey_Empty() throws IOException{
    	StubTelemetry telemetry = new StubTelemetry("Test Base Telemetry");
    	telemetry.setTimestamp(new Date());
    	
        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        telemetry.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        int index = asJson.indexOf("\"name\":\"Microsoft.ApplicationInsights.Stub\"");
        assertTrue(index != -1);
    }
    

}