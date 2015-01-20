package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

public final class InternalContextTest {
    @Test
    public void testSetAgentVersion() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        InternalContext context = new InternalContext(map);
        context.setAgentVersion("mock");

        assertEquals(context.getAgentVersion(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getInternalAgentVersion()), "mock");
    }

    @Test
    public void testSetSdkVersion() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        InternalContext context = new InternalContext(map);
        context.setSdkVersion("mock");

        assertEquals(context.getSdkVersion(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getInternalSdkVersion()), "mock");
    }
}