package com.microsoft.applicationinsights.internal.schemav2;

import com.google.gson.Gson;
import com.microsoft.applicationinsights.telemetry.DependencyKind;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class RemoteDependencyDataTest {
    @Test
    public void testSerialize() throws IOException {
        RemoteDependencyData rdd = new RemoteDependencyData();
        rdd.setName("name");
        rdd.setAsync(true);
        rdd.setCount(100);
        rdd.setDependencyKind(DependencyKind.HttpOnly);
        rdd.setDependencySource(DependencySourceType.Apmc);
        rdd.setKind(DataPointType.Aggregation);
        rdd.setSuccess(false);
        rdd.setValue(1000.1);

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        rdd.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        asJson = intToEnum(asJson, "\"dependencyKind\":\"1\"", "\"dependencyKind\":\"HttpOnly\"");
        asJson = intToEnum(asJson, "\"dependencySource\":\"2\"", "\"dependencySource\":\"Apmc\"");
        asJson = intToEnum(asJson, "\"kind\":\"1\"", "\"kind\":\"Aggregation\"");

        Gson gson = new Gson();
        RemoteDependencyData rddFromJson = gson.fromJson(asJson, RemoteDependencyData.class);
        assertEquals(rddFromJson, rdd);
    }

    private static String intToEnum(String jsonString, String toReplace, String toInsert) {
        int index = jsonString.indexOf(toReplace);
        assertTrue(index != -1);
        return jsonString.substring(0, index) + toInsert + jsonString.substring(index + toReplace.length());
    }
}
