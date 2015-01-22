package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class UserContextTest {
    @Test
    public void testSetId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setId("mock");

        assertEquals(context.getId(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getUserId()), "mock");
    }

    @Test
    public void testSetAccountId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setAccountId("mock");

        assertEquals(context.getAccountId(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getUserAccountId()), "mock");
    }

    @Test
    public void testSetAcquisitionDate() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        Date date = new Date();
        context.setAcquisitionDate(date);

        assertEquals(context.getAcquisitionDate(), date);
        assertEquals(map.size(), 1);
    }

    @Test
    public void testSetAcquisitionNullDate() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setAcquisitionDate(null);

        assertNull(context.getAcquisitionDate());
        assertEquals(map.size(), 0);
        assertNull(map.get(ContextTagKeys.getKeys().getUserAccountAcquisitionDate()));
    }

    @Test
    public void testSetUserAgent() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setUserAgent("mock");

        assertEquals(context.getUserAgent(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getUserAgent()), "mock");
    }
}