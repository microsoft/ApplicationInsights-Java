package com.microsoft.applicationinsights.smoketest;

import java.util.Comparator;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.*;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent("logging")
public class TraceJavaUtilLoggingTest extends AiSmokeTest {

    @Test
    @TargetUri("/traceJavaUtilLogging")
    public void testTraceJavaUtilLogging() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForMessageItemsInRequest(2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope mdEnvelope1 = mdList.get(0);
        Envelope mdEnvelope2 = mdList.get(1);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
        logs.sort(new Comparator<MessageData>() {
            @Override
            public int compare(MessageData o1, MessageData o2) {
                return o1.getSeverityLevel().compareTo(o2.getSeverityLevel());
            }
        });

        MessageData md2 = logs.get(1);
        MessageData md3 = logs.get(2);

        assertEquals("This is jul warning.", md2.getMessage());
        assertEquals(SeverityLevel.Warning, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("WARNING", md2.getProperties().get("LoggingLevel"));
        assertParentChild(rd, rdEnvelope, mdEnvelope1);

        assertEquals("This is jul severe.", md3.getMessage());
        assertEquals(SeverityLevel.Error, md3.getSeverityLevel());
        assertEquals("Logger", md3.getProperties().get("SourceType"));
        assertEquals("SEVERE", md3.getProperties().get("LoggingLevel"));
        assertParentChild(rd, rdEnvelope, mdEnvelope2);
    }

    @Test
    @TargetUri("traceJavaUtilLoggingWithException")
    public void testTraceJavaUtilLoggingWithExeption() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> edList = mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

        Envelope edEnvelope = edList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        ExceptionData ed = (ExceptionData) ((Data) edEnvelope.getData()).getBaseData();

        List<ExceptionDetails> details = ed.getExceptions();
        ExceptionDetails ex = details.get(0);

        assertEquals("Fake Exception", ex.getMessage());
        assertEquals(SeverityLevel.Error, ed.getSeverityLevel());
        assertEquals("This is an exception!", ed.getProperties().get("Logger Message"));
        assertEquals("Logger", ed.getProperties().get("SourceType"));
        assertEquals("SEVERE", ed.getProperties().get("LoggingLevel"));
        assertParentChild(rd, rdEnvelope, edEnvelope);
    }

    private static void assertParentChild(RequestData rd, Envelope rdEnvelope, Envelope childEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        assertNotNull(operationId);
        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));

        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");
        assertNull(operationParentId);

        assertEquals(rd.getId(), childEnvelope.getTags().get("ai.operation.parentId"));
    }
}