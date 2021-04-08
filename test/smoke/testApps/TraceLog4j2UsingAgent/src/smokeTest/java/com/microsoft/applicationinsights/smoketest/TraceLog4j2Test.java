package com.microsoft.applicationinsights.smoketest;

import java.util.Comparator;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent("logging")
public class TraceLog4j2Test extends AiSmokeTest {

    @Test
    @TargetUri("/traceLog4j2")
    public void testTraceLog4j2() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForMessageItemsInRequest(3);

        Envelope rdEnvelope = rdList.get(0);
        Envelope mdEnvelope1 = mdList.get(0);
        Envelope mdEnvelope2 = mdList.get(1);
        Envelope mdEnvelope3 = mdList.get(2);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
        logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

        MessageData md1 = logs.get(0);
        MessageData md2 = logs.get(1);
        MessageData md3 = logs.get(2);

        assertEquals("This is log4j2 warn.", md1.getMessage());
        assertEquals(SeverityLevel.Warning, md1.getSeverityLevel());
        assertEquals("Logger", md1.getProperties().get("SourceType"));
        assertEquals("WARN", md1.getProperties().get("LoggingLevel"));
        assertEquals("smoketestapp", md1.getProperties().get("LoggerName"));
        assertEquals("MDC value", md1.getProperties().get("MDC key"));
        assertEquals(4, md1.getProperties().size());

        assertEquals("This is log4j2 error.", md2.getMessage());
        assertEquals(SeverityLevel.Error, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("ERROR", md2.getProperties().get("LoggingLevel"));
        assertEquals("smoketestapp", md1.getProperties().get("LoggerName"));
        assertEquals(3, md2.getProperties().size());

        assertEquals("This is log4j2 fatal.", md3.getMessage());
        assertEquals(SeverityLevel.Critical, md3.getSeverityLevel());
        assertEquals("Logger", md3.getProperties().get("SourceType"));
        assertEquals("FATAL", md3.getProperties().get("LoggingLevel"));
        assertEquals("smoketestapp", md3.getProperties().get("LoggerName"));
        assertEquals(3, md3.getProperties().size());

        assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /TraceLog4j2UsingAgent/traceLog4j2");
        assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /TraceLog4j2UsingAgent/traceLog4j2");
        assertParentChild(rd, rdEnvelope, mdEnvelope3, "GET /TraceLog4j2UsingAgent/traceLog4j2");
    }

    @Test
    @TargetUri("/traceLog4j2WithException")
    public void testTraceLog4j2WithException() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> edList = mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope edEnvelope = edList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

        List<ExceptionDetails> details = ed.getExceptions();
        ExceptionDetails ex = details.get(0);

        assertEquals("Fake Exception", ex.getMessage());
        assertEquals(SeverityLevel.Error, ed.getSeverityLevel());
        assertEquals("This is an exception!", ed.getProperties().get("Logger Message"));
        assertEquals("Logger", ed.getProperties().get("SourceType"));
        assertEquals("ERROR", ed.getProperties().get("LoggingLevel"));
        assertEquals("smoketestapp", ed.getProperties().get("LoggerName"));
        assertEquals("MDC value", ed.getProperties().get("MDC key"));
        assertEquals(5, ed.getProperties().size());

        assertParentChild(rd, rdEnvelope, edEnvelope, "GET /TraceLog4j2UsingAgent/traceLog4j2WithException");
    }

    private static void assertParentChild(RequestData rd, Envelope rdEnvelope, Envelope childEnvelope, String operationName) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        assertNotNull(operationId);
        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));

        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");
        assertNull(operationParentId);

        assertEquals(rd.getId(), childEnvelope.getTags().get("ai.operation.parentId"));

        assertEquals(operationName, rdEnvelope.getTags().get("ai.operation.name"));
        assertNull(childEnvelope.getTags().get("ai.operation.name"));
    }
}
