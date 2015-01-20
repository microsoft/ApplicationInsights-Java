package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

public final class ComponentContextTest {
    @Test
    public void testSetVersion() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        ComponentContext context = new ComponentContext(map);
        context.setVersion("version1");

        assertEquals(context.getVersion(), "version1");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getApplicationVersion()), "version1");
    }
}