package com.microsoft.applicationinsights.extensibility.context;

import org.junit.*;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

public class CloudContextTest {
    @Test
    public void testSetRoleInstance() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        CloudContext context = new CloudContext(map);
        final String expected = "mock-instance";
        context.setRoleInstance(expected);

        assertEquals(expected, context.getRoleInstance());
        assertEquals(1, map.size());
        assertEquals(expected, map.get(ContextTagKeys.getKeys().getCloudRoleInstance()));
    }

    @Test
    public void testSetRoleName() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        CloudContext context = new CloudContext(map);
        final String expected = "mock-role";
        context.setRole(expected);

        assertEquals(expected, context.getRole());
        assertEquals(1, map.size());
        assertEquals(expected, map.get(ContextTagKeys.getKeys().getCloudRole()));
    }
}
