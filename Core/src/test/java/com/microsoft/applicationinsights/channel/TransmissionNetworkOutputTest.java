package com.microsoft.applicationinsights.channel;

import org.junit.Test;

public class TransmissionNetworkOutputTest {

    @Test(expected = NullPointerException.class)
    public void testSetTransmissionDispatcherWithNullUri() throws Exception {
        new TransmissionNetworkOutput(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTransmissionDispatcherWithEmptyUri() throws Exception {
        new TransmissionNetworkOutput("");
    }
}