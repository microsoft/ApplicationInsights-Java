package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
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
    public void customInstrumentationOne() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals(rdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.one");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/customInstrumentationTwo")
    public void customInstrumentationTwo() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals(rdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.two");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/customInstrumentationThree")
    public void customInstrumentationThree() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);
        List<Envelope> edList = mockedIngestion.waitForItems("ExceptionData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);
        Envelope edEnvelope = edList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();
        ExceptionData ed = (ExceptionData) ((Data) edEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals(rdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.three");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), false);

        List<ExceptionDetails> exceptions = ed.getExceptions();
        assertEquals(exceptions.size(), 1);
        assertEquals(exceptions.get(0).getMessage(), "Three");

        assertSameOperationId(rdEnvelope, rddEnvelope);
        assertSameOperationId(edEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/customInstrumentationFour")
    public void customInstrumentationFour() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals(rdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject$NestedObject.four");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/customInstrumentationFive")
    public void customInstrumentationFive() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 4);

        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        Envelope fiveEnvelope = null;
        Envelope sixEnvelope = null;
        Envelope oneEnvelope = null;
        Envelope twoEnvelope = null;
        RemoteDependencyData fiveRdd = null;
        RemoteDependencyData sixRdd = null;
        RemoteDependencyData oneRdd = null;
        RemoteDependencyData twoRdd = null;
        for (Envelope loopEnvelope : rddList) {
            RemoteDependencyData loopData = (RemoteDependencyData) ((Data) loopEnvelope.getData()).getBaseData();
            if (loopData.getName().endsWith(".five")) {
                fiveEnvelope = loopEnvelope;
                fiveRdd = loopData;
            } else if (loopData.getName().endsWith(".six")) {
                sixEnvelope = loopEnvelope;
                sixRdd = loopData;
            } else if (loopData.getName().endsWith(".one")) {
                oneEnvelope = loopEnvelope;
                oneRdd = loopData;
            } else if (loopData.getName().endsWith(".two")) {
                twoEnvelope = loopEnvelope;
                twoRdd = loopData;
            } else {
                throw new IllegalStateException("Unexpected remote dependency: " + loopData.getName());
            }
        }

        assertTrue(rd.getSuccess());

        assertNotNull(fiveRdd);
        assertEquals(fiveRdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.five");
        assertEquals(fiveRdd.getType(), "OTHER");
        assertEquals(fiveRdd.getSuccess(), true);
        assertSameOperationId(rdEnvelope, fiveEnvelope);

        assertNotNull(sixRdd);
        assertEquals(sixRdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.six");
        assertEquals(sixRdd.getType(), "OTHER");
        assertEquals(sixRdd.getSuccess(), true);
        assertSameOperationId(rdEnvelope, sixEnvelope);

        assertNotNull(oneRdd);
        assertEquals(oneRdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.one");
        assertEquals(oneRdd.getType(), "OTHER");
        assertEquals(oneRdd.getSuccess(), true);
        assertSameOperationId(rdEnvelope, oneEnvelope);

        assertNotNull(twoRdd);
        assertEquals(twoRdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.two");
        assertEquals(twoRdd.getType(), "OTHER");
        assertEquals(twoRdd.getSuccess(), true);
        assertSameOperationId(rdEnvelope, twoEnvelope);
    }

    @Test
    @TargetUri("/customInstrumentationSeven")
    public void customInstrumentationSeven() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals(rdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.seven");
        assertEquals(rdd.getType(), "OTHER");
        assertEquals(rdd.getSuccess(), true);
        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/customInstrumentationEight")
    public void customInstrumentationEight() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope1 = rddList.get(0);
        Envelope rddEnvelope2 = rddList.get(1);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data) rddEnvelope2.getData()).getBaseData();

        assertTrue(rd.getSuccess());

        assertEquals(rdd1.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.eight");
        assertEquals(rdd1.getType(), "OTHER");
        assertEquals(rdd1.getSuccess(), true);
        assertSameOperationId(rdEnvelope, rddEnvelope1);

        assertEquals(rdd2.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.eight");
        assertEquals(rdd2.getType(), "OTHER");
        assertEquals(rdd2.getSuccess(), true);
        assertSameOperationId(rdEnvelope, rddEnvelope2);
    }

    @Test
    @TargetUri("/customInstrumentationNine")
    public void customInstrumentationNine() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 2);

        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        Envelope nineEnvelope = null;
        Envelope httpEnvelope = null;
        RemoteDependencyData nineRdd = null;
        RemoteDependencyData httpRdd = null;
        for (Envelope loopEnvelope : rddList) {
            RemoteDependencyData loopData = (RemoteDependencyData) ((Data) loopEnvelope.getData()).getBaseData();
            if (loopData.getType().equals("OTHER")) {
                nineEnvelope = loopEnvelope;
                nineRdd = loopData;
            } else if (loopData.getType().equals("Http (tracked component)")) {
                httpEnvelope = loopEnvelope;
                httpRdd = loopData;
            } else {
                throw new IllegalStateException("Unexpected remote dependency type: " + loopData.getType());
            }
        }

        assertTrue(rd.getSuccess());

        assertNotNull(nineRdd);
        assertEquals(nineRdd.getName(), "com/microsoft/applicationinsights/smoketestapp/TargetObject.nine");
        assertEquals(nineRdd.getType(), "OTHER");
        assertEquals(nineRdd.getSuccess(), true);
        assertSameOperationId(rdEnvelope, nineEnvelope);

        assertNotNull(httpRdd);
        assertSameOperationId(rdEnvelope, httpEnvelope);
    }

    private static void assertSameOperationId(Envelope rdEnvelope, Envelope rddEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");

        assertNotNull(operationId);
        assertNotNull(operationParentId);

        assertEquals(operationId, rddEnvelope.getTags().get("ai.operation.id"));
        assertEquals(operationParentId, rddEnvelope.getTags().get("ai.operation.parentId"));
    }
}
