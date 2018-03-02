package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.*;

@Ignore
public class SimpleTrackExceptionDataTest extends AiSmokeTest {

    @Test
    @TargetUri("/trackException?leftOperand=1&rightOperand=2&operator=plus")
    public void testTrackException() throws Exception {

        assertEquals(2, mockedIngestion.getCountForType("RequestData"));
        assertEquals(3, mockedIngestion.getCountForType("ExceptionData"));
        int totalItems = mockedIngestion.getItemCount();
        int expectedItems = 5;
        assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);

        final String expectedName = "This is track exception.";
        final String expectedProperties = "value";
        final Double expectedMetrice = 1d;

        ExceptionData d = getTelemetryDataForType(0, "ExceptionData");
        ExceptionDetails eDetails = getDetails(d);
        assertEquals(expectedName, eDetails.getMessage());

        ExceptionData d2 = getTelemetryDataForType(1, "ExceptionData");
        ExceptionDetails eDetails2 = getDetails(d2);
        assertEquals(expectedName, eDetails2.getMessage());
        assertEquals(expectedProperties, d2.getProperties().get("key"));
        assertEquals(expectedMetrice, d2.getMeasurements().get("key"));

        ExceptionData d3 = getTelemetryDataForType(2, "ExceptionData");
        ExceptionDetails eDetails3 = getDetails(d3);
        assertEquals(expectedName, eDetails3.getMessage());
        assertEquals(SeverityLevel.Error, d3.getSeverityLevel());
    }

    private ExceptionDetails getDetails(ExceptionData exceptionData) {
        List<ExceptionDetails> details = exceptionData.getExceptions();
        ExceptionDetails ex = details.get(0);
        return ex;
    }

}