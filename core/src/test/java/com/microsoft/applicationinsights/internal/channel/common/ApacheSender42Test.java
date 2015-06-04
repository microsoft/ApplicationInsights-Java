package com.microsoft.applicationinsights.internal.channel.common;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ApacheSender42Test {
    @Test
    public void testHttpClientType() {
        HttpClient tested = new ApacheSender42().getHttpClient();
        assertNotNull(tested);
        assertTrue(tested instanceof DefaultHttpClient);
    }
}
