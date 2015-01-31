package com.microsoft.applicationinsights.internal.schemav2;

import com.google.gson.Gson;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public final class MessageDataTest {
    @Test
    public void testSerialize() throws IOException {
        MessageData messageData = new MessageData();
        messageData.setMessage("My Message");
        messageData.setSeverityLevel(SeverityLevel.Information);

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        messageData.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        Gson gson = new Gson();
        MessageData messageDataFromJson = gson.fromJson(asJson, MessageData.class);
        assertEquals(messageDataFromJson, messageData);
    }
}
