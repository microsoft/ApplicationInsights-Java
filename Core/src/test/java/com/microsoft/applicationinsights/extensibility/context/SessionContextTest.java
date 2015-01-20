package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class SessionContextTest {
    @Test
    public void testSetId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setId("mock");

        assertEquals(context.getId(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getSessionId()), "mock");
    }

    @Test
    public void testSetIsFirstNull() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsFirst(null);

        assertNull(context.getIsFirst());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getSessionIsFirst()));
    }

    @Test
    public void testSetIsFirstTrue() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsFirst(true);

        assertEquals(context.getIsFirst(), true);
        assertEquals(map.size(), 1);
        assertEquals(Boolean.valueOf(map.get(ContextTagKeys.getKeys().getSessionIsFirst())), true);
    }

    @Test
    public void testSetIsNewSessionNull() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsNewSession(null);

        assertNull(context.getIsNewSession());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getSessionIsNew()));
    }

    @Test
    public void testSetIsNewSessionTrue() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        SessionContext context = new SessionContext(map);
        context.setIsNewSession(true);

        assertEquals(context.getIsNewSession(), true);
        assertEquals(map.size(), 1);
        assertEquals(Boolean.valueOf(map.get(ContextTagKeys.getKeys().getSessionIsNew())), true);
    }
}