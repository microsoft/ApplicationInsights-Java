package com.microsoft.applicationinsights.channel;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransmissionTest {
    private final static String MOCK_WEB_CONTENT_TYPE = "MockContent";
    private final static String MOCK_WEB_ENCODING_TYPE = "MockEncoding";

    @Test(expected = IllegalArgumentException.class)
    public void testNullContentType() throws Exception {
        byte[] mockContent = new byte[2];
        new Transmission(mockContent, null, MOCK_WEB_ENCODING_TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyContentType() throws Exception {
        byte[] mockContent = new byte[2];
        new Transmission(mockContent, "", MOCK_WEB_ENCODING_TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullContentEncodingType() throws Exception {
        byte[] mockContent = new byte[2];
        new Transmission(mockContent, MOCK_WEB_CONTENT_TYPE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyContentEncodingType() throws Exception {
        byte[] mockContent = new byte[2];
        new Transmission(mockContent, MOCK_WEB_CONTENT_TYPE, "");
    }

    @Test(expected = NullPointerException.class)
    public void testNullContent() throws Exception {
        new Transmission(null, MOCK_WEB_CONTENT_TYPE, MOCK_WEB_ENCODING_TYPE);
    }

    @Test
    public void testGetContent() throws Exception {
        byte[] mockContent = new byte[2];
        Transmission tested = new Transmission(mockContent, MOCK_WEB_CONTENT_TYPE, MOCK_WEB_ENCODING_TYPE);

        assertSame(mockContent, tested.getContent());
    }

    @Test
    public void testGetWebContentType() throws Exception {
        byte[] mockContent = new byte[2];
        Transmission tested = new Transmission(mockContent, MOCK_WEB_CONTENT_TYPE, MOCK_WEB_ENCODING_TYPE);

        assertEquals(MOCK_WEB_CONTENT_TYPE, tested.getWebContentType());
    }

    @Test
    public void testGetWebContentEncodingType() throws Exception {
        byte[] mockContent = new byte[2];
        Transmission tested = new Transmission(mockContent, MOCK_WEB_CONTENT_TYPE, MOCK_WEB_ENCODING_TYPE);

        assertEquals(MOCK_WEB_ENCODING_TYPE, tested.getWebContentEncodingType());
    }
}