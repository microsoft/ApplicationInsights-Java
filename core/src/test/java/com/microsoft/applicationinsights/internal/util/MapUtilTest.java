package com.microsoft.applicationinsights.internal.util;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MapUtilTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void targetCannotBeNullInCopy() {
        expected.expect(IllegalArgumentException.class);
        MapUtil.copy(new HashMap<String, String>(), null);
    }

    @Test
    public void copyIsNoOpIfSourceIsNullOrEmpty() {
        Map<String, String> source = mock(Map.class);
        Map<String, String> target = mock(Map.class);
        when(source.size()).thenReturn(0);

        MapUtil.copy(source, target);
        // nothing should be put into target
        verify(target, never()).put(anyString(), anyString());
        verify(source, never()).get(any());

        reset(target);

        MapUtil.copy(null, target);
        verify(target, never()).put(anyString(), anyString());
    }

    @Test
    public void testCopyIntoHashMap() {
        Map<String, String> source = new HashMap<>();
        Map<String, String> target = new HashMap<>();

        source.put("key1", "value1");
        source.put("key2", null);

        MapUtil.copy(source, target);
        assertEquals(2, target.size());
    }

    @Test
    public void testCopyIntoConcurrentHashMap() {
        Map<String, String> source = new HashMap<>();
        Map<String, String> target = new ConcurrentHashMap<>();

        source.put("key1", "value1");
        source.put("key2", null);

        MapUtil.copy(source, target);
        assertEquals(1, target.size());
    }
}
