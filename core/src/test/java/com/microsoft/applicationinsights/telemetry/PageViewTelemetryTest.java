package com.microsoft.applicationinsights.telemetry;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PageViewTelemetryTest {
    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";

    @Test
    public void testEmptyCtor() {
        PageViewTelemetry telemetry = new PageViewTelemetry();

        assertNull(telemetry.getName());
        assertNull(telemetry.getUri());
        assertTrue(telemetry.getMetrics().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertEquals(telemetry.getDuration(), 0);
    }

    @Test
    public void testCtor() {
        PageViewTelemetry telemetry = new PageViewTelemetry("MockName");

        assertEquals(telemetry.getName(), "MockName");
        assertNull(telemetry.getUri());
        assertTrue(telemetry.getMetrics().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertEquals(telemetry.getDuration(), 0);
    }

    @Test
    public void testSetName() {
        PageViewTelemetry telemetry = new PageViewTelemetry("MockName");

        telemetry.setName("MockName1");
        assertEquals(telemetry.getName(), "MockName1");
    }

    @Test
    public void testSetDuration() {
        PageViewTelemetry telemetry = new PageViewTelemetry("MockName");

        telemetry.setDuration(2001);
        assertEquals(telemetry.getDuration(), 2001);
    }

    @Test
    public void testSetUri() throws URISyntaxException {
        PageViewTelemetry telemetry = new PageViewTelemetry();

        URI uri = new URI("http://microsoft.com/");
        telemetry.setUrl(uri);
        assertEquals(telemetry.getUri(), uri);
    }
}
