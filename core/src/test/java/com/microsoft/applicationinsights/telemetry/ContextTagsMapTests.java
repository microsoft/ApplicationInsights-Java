package com.microsoft.applicationinsights.telemetry;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.extensibility.context.ContextTagKeys;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import static org.junit.Assert.*;

public class ContextTagsMapTests {
    private ContextTagsMap map;

    @Before
    public void setup() {
        map = new ContextTagsMap();
    }

    @After
    public void tearDown() {
        map = null;
    }


    @Test
    public void putTruncatesValueOverLimit() {
        // max len is 64
        String sessionIdKey = ContextTagKeys.getKeys().getSessionId();
        String value = StringUtils.repeat("1234", 32);
        map.put(sessionIdKey, value);
        assertEquals(StringUtils.repeat("1234", 16), map.get(sessionIdKey));
    }

    @Test
    public void putAllTruncatesValuesOverLimit() {
        final String locationIpValue = StringUtils.repeat('x', 55); // max=45
        final String deviceIdValue = StringUtils.repeat('y', 2048); //max=1024
        final String operationParentIdValue = StringUtils.repeat('z', 127); //max=128, this one is intentionally within the limits

        final String customKey = "not a limited key";
        final String customValue = StringUtils.repeat("1234", 1024);

        Map<String, String> kvps = new HashMap<>();
        kvps.put(ContextTagKeys.getKeys().getLocationIP(), locationIpValue);
        kvps.put(ContextTagKeys.getKeys().getDeviceId(), deviceIdValue);
        kvps.put(ContextTagKeys.getKeys().getOperationParentId(), operationParentIdValue);
        kvps.put(customKey, customValue);

        map.putAll(kvps);

        assertEquals(StringUtils.repeat('x', 45), map.get(ContextTagKeys.getKeys().getLocationIP()));
        assertEquals(StringUtils.repeat('y', 1024), map.get(ContextTagKeys.getKeys().getDeviceId()));
        assertEquals(operationParentIdValue, map.get(ContextTagKeys.getKeys().getOperationParentId()));
        assertEquals(customValue, map.get(customKey));
    }

}
