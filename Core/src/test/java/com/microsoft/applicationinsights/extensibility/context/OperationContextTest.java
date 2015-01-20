package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

public final class OperationContextTest {
    @Test
    public void testSetName() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        OperationContext context = new OperationContext(map);
        context.setName("mock");

        assertEquals(context.getName(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getOperationName()), "mock");
    }

    @Test
    public void testSetId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        OperationContext context = new OperationContext(map);
        context.setId("mock");

        assertEquals(context.getId(), "mock");
        assertEquals(map.size(), 1);
        assertEquals(map.get(ContextTagKeys.getKeys().getOperationId()), "mock");
    }
}
