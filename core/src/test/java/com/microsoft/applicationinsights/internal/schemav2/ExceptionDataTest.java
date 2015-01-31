package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.applicationinsights.telemetry.ExceptionHandledAt;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.junit.Test;

import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;

public final class ExceptionDataTest {
    @Test
    public void testSerialize() throws IOException {
        ExceptionData data = new ExceptionData();
        data.setSeverityLevel(SeverityLevel.Error);
        ArrayList<ExceptionDetails> exceptions = new ArrayList<ExceptionDetails>();
        exceptions.add(new ExceptionDetails());
        data.setExceptions(exceptions);
        data.setHandledAt(ExceptionHandledAt.UserCode.toString());

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        data.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        Gson gson = new Gson();
        ExceptionData dataFromJson = gson.fromJson(asJson, ExceptionData.class);
        assertEquals(dataFromJson, data);
    }
}