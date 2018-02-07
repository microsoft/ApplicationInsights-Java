package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.telemetry.Duration;

import org.junit.Test;

public class SimpleTrackDependencyDataTest extends AiSmokeTest {

    @Test
    @TargetUri("/trackDependency?leftOperand=1&rightOperand=2&operator=plus")
    public void trackDependency() throws Exception {
        assertEquals(2, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 3;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);
                
        // TODO get dependency data envelope and verify value
        RemoteDependencyData d = getTelemetryDataForType(0, "RemoteDependencyData");

        final String expectedName = "DependencyTest";
        final String expectedData = "commandName";
        final Duration expectedDuration = new Duration(0, 0, 1, 1, 1);

        assertEquals(expectedName, d.getName());
        assertEquals(expectedData, d.getData());
        assertEquals(expectedDuration, d.getDuration());  
    }
}
