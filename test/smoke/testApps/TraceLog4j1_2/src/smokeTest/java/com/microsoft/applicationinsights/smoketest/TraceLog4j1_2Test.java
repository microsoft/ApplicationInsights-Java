package com.microsoft.applicationinsights.smoketest;

import java.util.Comparator;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.*;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent("logging")
public class TraceLog4j1_2Test extends AiSmokeTest {

    @Test
    @TargetUri("/traceLog4j1_2")
    public void testTraceLog4j1_2() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForMessageItemsInRequest(3);

        Envelope rdEnvelope = rdList.get(0);
        Envelope mdEnvelope1 = mdList.get(0);
        Envelope mdEnvelope2 = mdList.get(1);
        Envelope mdEnvelope3 = mdList.get(2);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
        logs.sort(new Comparator<MessageData>() {
            @Override
            public int compare(MessageData o1, MessageData o2) {
                return o1.getSeverityLevel().compareTo(o2.getSeverityLevel());
            }
        });

        MessageData md1 = logs.get(0);
        MessageData md2 = logs.get(1);
        MessageData md3 = logs.get(3);

        assertEquals("This is log4j1.2 warn.", md1.getMessage());
        assertEquals(SeverityLevel.Warning, md1.getSeverityLevel());
        assertEquals("Logger", md1.getProperties().get("SourceType"));
        assertEquals("WARN", md1.getProperties().get("LoggingLevel"));
        assertEquals("MDC value", md1.getProperties().get("MDC key"));
        assertParentChild(rd, rdEnvelope, mdEnvelope1);

        assertEquals("This is log4j1.2 error.", md2.getMessage());
        assertEquals(SeverityLevel.Error, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("ERROR", md2.getProperties().get("LoggingLevel"));
        assertFalse(md2.getProperties().containsKey("MDC key"));
        assertParentChild(rd, rdEnvelope, mdEnvelope2);

        assertEquals("This is log4j1.2 fatal.", md3.getMessage());
        assertEquals(SeverityLevel.Critical, md3.getSeverityLevel());
        assertEquals("Logger", md3.getProperties().get("SourceType"));
        assertEquals("FATAL", md3.getProperties().get("LoggingLevel"));
        assertFalse(md3.getProperties().containsKey("MDC key"));
        assertParentChild(rd, rdEnvelope, mdEnvelope3);
    }

    @Test
    @TargetUri("traceLog4j1_2WithException")
    public void testTraceLog4j1_2WithExeption() throws Exception {
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
        assertEquals("ERROR", ed.getProperties().get("LoggingLevel"));
        assertEquals("MDC value", ed.getProperties().get("MDC key"));
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