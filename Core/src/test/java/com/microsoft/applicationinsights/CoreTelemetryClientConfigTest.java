package com.microsoft.applicationinsights;

import com.microsoft.applicationinsights.channel.IContextConfig;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CoreTelemetryClientConfigTest extends TestCase {

    CoreTelemetryClientConfig config;

    public void setUp() throws Exception {
        super.setUp();
        this.config = new CoreTelemetryClientConfig("ikey");
    }

    public void tearDown() throws Exception {

    }

    public void testGetInstrumentationKey() throws Exception {
        Assert.assertEquals("Ikey is set", "ikey", this.config.getInstrumentationKey());
    }

    public void testGetAccountId() throws Exception {
        Assert.assertEquals("Account ID is set", null, this.config.getAccountId());
    }

    public void testGetSessionRenewalMs() throws Exception {
        Assert.assertEquals("SessionRenewal is set", IContextConfig.defaultSessionRenewalMs,
                this.config.getSessionRenewalMs());
    }

    public void testGetSessionExpirationMs() throws Exception {
        Assert.assertEquals("SessionExpiry is set", IContextConfig.defaultSessionExpirationMs,
                this.config.getSessionExpirationMs());
    }

    public void testGetSenderConfig() throws Exception {
        Assert.assertNotNull("Sender config is not null", this.config.getSenderConfig());
    }
}