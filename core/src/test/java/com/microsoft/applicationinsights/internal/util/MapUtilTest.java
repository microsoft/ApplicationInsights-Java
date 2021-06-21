package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class MapUtilTest {

    @Test
    public void targetCannotBeNullInCopy() {
        assertThatThrownBy(() -> MapUtil.copy(new HashMap<String, String>(), null))
                .isInstanceOf(IllegalArgumentException.class);
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
