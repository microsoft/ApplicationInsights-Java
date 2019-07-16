package com.springbootstartertest.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseAgent("CustomInstrumentation")
public class CustomInstrumentationTest extends AiSmokeTest {

    @Test
    @TargetUri("/customInstrumentationOne")
    public void customInstrumentationOne() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals(rdd.getName(), "com/springbootstartertest/controller/TargetObject.one");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);
    }

    @Test
    @TargetUri("/customInstrumentationTwo")
    public void customInstrumentationTwo() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals(rdd.getName(), "com/springbootstartertest/controller/TargetObject.two");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);
    }

    @Test
    @TargetUri("/customInstrumentationThree")
    public void customInstrumentationThree() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        assertEquals(1, mockedIngestion.getCountForType("ExceptionData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals(rdd.getName(), "com/springbootstartertest/controller/TargetObject.three");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), false);
        ExceptionData exceptionData = getTelemetryDataForType(0, "ExceptionData");
        List<ExceptionDetails> exceptions = exceptionData.getExceptions();
        assertEquals(exceptions.size(), 1);
        assertEquals(exceptions.get(0).getMessage(), "Three");
    }

    @Test
    @TargetUri("/customInstrumentationFour")
    public void customInstrumentationFour() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals(rdd.getName(), "com/springbootstartertest/controller/TargetObject$NestedObject.four");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);
    }

    @Test
    @TargetUri("/customInstrumentationFive")
    public void customInstrumentationFive() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(4, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData fiveRdd = null;
        RemoteDependencyData sixRdd = null;
        RemoteDependencyData oneRdd = null;
        RemoteDependencyData twoRdd = null;
        List<RemoteDependencyData> rdds = mockedIngestion.getTelemetryDataByType("RemoteDependencyData");
        for (RemoteDependencyData rdd : rdds) {
            if (rdd.getName().endsWith(".five")) {
                fiveRdd = rdd;
            } else if (rdd.getName().endsWith(".six")) {
                sixRdd = rdd;
            } else if (rdd.getName().endsWith(".one")) {
                oneRdd = rdd;
            } else if (rdd.getName().endsWith(".two")) {
                twoRdd = rdd;
            } else {
                throw new IllegalStateException("Unexpected remote dependency: " + rdd.getName());
            }
        }

        assertNotNull(fiveRdd);
        assertEquals(fiveRdd.getName(), "com/springbootstartertest/controller/TargetObject.five");
        assertEquals(fiveRdd.getType(), "OTHER");
        assertEquals(fiveRdd.getSuccess(), true);

        assertNotNull(sixRdd);
        assertEquals(sixRdd.getName(), "com/springbootstartertest/controller/TargetObject.six");
        assertEquals(sixRdd.getType(), "OTHER");
        assertEquals(sixRdd.getSuccess(), true);

        assertNotNull(oneRdd);
        assertEquals(oneRdd.getName(), "com/springbootstartertest/controller/TargetObject.one");
        assertEquals(oneRdd.getType(), "OTHER");
        assertEquals(oneRdd.getSuccess(), true);

        assertNotNull(twoRdd);
        assertEquals(twoRdd.getName(), "com/springbootstartertest/controller/TargetObject.two");
        assertEquals(twoRdd.getType(), "OTHER");
        assertEquals(twoRdd.getSuccess(), true);
    }

    @Test
    @TargetUri("/customInstrumentationSeven")
    public void customInstrumentationSeven() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals(rdd.getName(), "com/springbootstartertest/controller/TargetObject.seven");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);
    }

    @Test
    @TargetUri("/customInstrumentationEight")
    public void customInstrumentationEight() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd1 = getTelemetryDataForType(0, "RemoteDependencyData");
        RemoteDependencyData rdd2 = getTelemetryDataForType(1, "RemoteDependencyData");

        assertEquals(rdd1.getName(), "com/springbootstartertest/controller/TargetObject.eight");
        assertEquals(rdd1.getType(), "OTHER");
        assertEquals(rdd1.getSuccess(), true);

        assertEquals(rdd2.getName(), "com/springbootstartertest/controller/TargetObject.eight");
        assertEquals(rdd2.getType(), "OTHER");
        assertEquals(rdd2.getSuccess(), true);
    }

    @Test
    @TargetUri("/customInstrumentationNine")
    public void customInstrumentationNine() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData nineRdd = null;
        RemoteDependencyData httpRdd = null;
        List<RemoteDependencyData> rdds = mockedIngestion.getTelemetryDataByType("RemoteDependencyData");
        for (RemoteDependencyData rdd : rdds) {
            if (rdd.getType().equals("OTHER")) {
                nineRdd = rdd;
            } else if (rdd.getType().equals("Http (tracked component)")) {
                httpRdd = rdd;
            } else {
                throw new IllegalStateException("Unexpected remote dependency type: " + rdd.getType());
            }
        }

        assertNotNull(nineRdd);
        assertEquals(nineRdd.getName(), "com/springbootstartertest/controller/TargetObject.nine");
        assertEquals(nineRdd.getType(), "OTHER");
        assertEquals(nineRdd.getSuccess(), true);

        assertNotNull(httpRdd);
        String requestOperationId = d.getId();
        String rddId = httpRdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }
}
