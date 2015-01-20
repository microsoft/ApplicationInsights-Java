package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class LocationContextTest {
    @Test
    public void testSetBadIp() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        LocationContext context = new LocationContext(map);
        context.setIp("a.255.255.255");

        assertNull(context.getIp());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getLocationIP()));
    }

    @Test
    public void testSetIp() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        LocationContext context = new LocationContext(map);
        context.setIp("127.255.255.255");

        assertEquals(context.getIp(), "127.255.255.255");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getLocationIP()), "127.255.255.255");
    }
}