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
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("logging")
public class TraceLogBackTest extends AiSmokeTest {

    @Test
    @TargetUri("/traceLogBack")
    public void testTraceLogBack() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForMessageItemsInRequest(2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope mdEnvelope1 = mdList.get(0);
        Envelope mdEnvelope2 = mdList.get(1);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
        logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

        MessageData md1 = logs.get(0);
        MessageData md2 = logs.get(1);

        assertEquals("This is logback warn.", md1.getMessage());
        assertEquals(SeverityLevel.Warning, md1.getSeverityLevel());
        assertEquals("Logger", md1.getProperties().get("SourceType"));
        assertEquals("WARN", md1.getProperties().get("LoggingLevel"));
        assertEquals("test", md1.getProperties().get("LoggerName"));
        // TODO add MDC instrumentation for jboss logging
        if (currentImageName.contains("wildfly")) {
            assertEquals(3, md1.getProperties().size());
        } else {
            assertEquals("MDC value", md1.getProperties().get("MDC key"));
            assertEquals(4, md1.getProperties().size());
        }

        assertEquals("This is logback error.", md2.getMessage());
        assertEquals(SeverityLevel.Error, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("ERROR", md2.getProperties().get("LoggingLevel"));
        assertEquals("test", md2.getProperties().get("LoggerName"));
        assertEquals(3, md2.getProperties().size());

        assertParentChild(rd, rdEnvelope, mdEnvelope1, "/TraceLogBackUsingAgent/traceLogBack");
        assertParentChild(rd, rdEnvelope, mdEnvelope2, "/TraceLogBackUsingAgent/traceLogBack");
    }

    @Test
    @TargetUri("traceLogBackWithException")
    public void testTraceLogBackWithExeption() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> edList = mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope edEnvelope = edList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

        assertEquals("Fake Exception", ed.getExceptions().get(0).getMessage());
        assertEquals(SeverityLevel.Error, ed.getSeverityLevel());
        assertEquals("This is an exception!", ed.getProperties().get("Logger Message"));
        assertEquals("Logger", ed.getProperties().get("SourceType"));
        assertEquals("ERROR", ed.getProperties().get("LoggingLevel"));
        assertEquals("test", ed.getProperties().get("LoggerName"));
        // TODO add MDC instrumentation for jboss logging
        if (currentImageName.contains("wildfly")) {
            assertEquals(4, ed.getProperties().size());
        } else {
            assertEquals("MDC value", ed.getProperties().get("MDC key"));
            assertEquals(5, ed.getProperties().size());
        }

        assertParentChild(rd, rdEnvelope, edEnvelope, "/TraceLogBackUsingAgent/traceLogBackWithException");
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