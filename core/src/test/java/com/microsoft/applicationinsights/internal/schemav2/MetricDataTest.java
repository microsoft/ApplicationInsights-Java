package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import org.junit.Test;

import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;

public final class MetricDataTest {
    @Test
    public void testSerialize() throws IOException {
        MetricData data = new MetricData();
        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        DataPoint dataPoint = new DataPoint();
        dataPoint.setName("name");
        dataPoint.setCount(127);
        dataPoint.setMax(1400.1);
        dataPoint.setMin(-1400.1);
        dataPoints.add(dataPoint);
        data.setMetrics(dataPoints);

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
        data.serialize(jsonWriter);
        jsonWriter.close();
        String asJson = writer.toString();

        Gson gson = new Gson();
        MetricData messageDataFromJson = gson.fromJson(asJson, MetricData.class);
        assertEquals(messageDataFromJson, data);
    }
}