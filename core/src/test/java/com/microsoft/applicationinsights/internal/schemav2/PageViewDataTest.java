package com.microsoft.applicationinsights.internal.schemav2;

import com.google.gson.Gson;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public final class PageViewDataTest {
    @Test
    public void testSerialize() throws IOException {
        PageViewData data = new PageViewData();
        data.setDuration(100L);
        data.setUrl("Url");
        data.setName("name");

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        data.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        Gson gson = new Gson();
        PageViewData dataFromJson = gson.fromJson(asJson, PageViewData.class);
        assertEquals(dataFromJson, data);
    }
}
