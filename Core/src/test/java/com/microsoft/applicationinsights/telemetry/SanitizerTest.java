package com.microsoft.applicationinsights.telemetry;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public final class SanitizerTest {
    private final static String VALID_KEY_PROPERTY = "ValidKey";
    private final static String VALID_VALUE_PROPERTY = "ValidValue";
    private final static String VALID_URL = "http://wwww.microsoft.com/";
    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";
    private final static Double VALID_VALUE_MEASUREMENT = 1.0;

    @Test
    public void testSanitizeNullProperties() throws Exception {
        Sanitizer.sanitizeProperties(null);
    }

    @Test
    public void testSanitizeEmptyProperties() throws Exception {
        ConcurrentMap<String, String> properties = new ConcurrentHashMap<String, String>();
        Sanitizer.sanitizeProperties(properties);

        assertTrue(properties.isEmpty());
    }

    @Test
    public void testSanitizeValidProperties() throws Exception {
        ConcurrentMap<String, String> properties = new ConcurrentHashMap<String, String>();
        properties.put(VALID_KEY_PROPERTY, VALID_VALUE_PROPERTY);
        Sanitizer.sanitizeProperties(properties);

        assertEquals(properties.size(), 1);
        assertEquals(properties.get(VALID_KEY_PROPERTY), VALID_VALUE_PROPERTY);
    }

    @Test
    public void testSanitizeTooLongKeyAndValueProperties() throws Exception {
        ConcurrentMap<String, String> properties = new ConcurrentHashMap<String, String>();

        properties.put(createString(Sanitizer.MAX_MAP_NAME_LENGTH + 1), createString(Sanitizer.MAX_VALUE_LENGTH + 1));
        Sanitizer.sanitizeProperties(properties);

        assertEquals(properties.size(), 1);
        Map.Entry<String, String> entry = properties.entrySet().iterator().next();
        assertEquals(entry.getKey().length(), Sanitizer.MAX_MAP_NAME_LENGTH);
        assertEquals(entry.getValue().length(), Sanitizer.MAX_VALUE_LENGTH);
    }

    @Test
    public void testSanitizeIllegalCharsProperties() throws Exception {
        ConcurrentMap<String, String> properties = new ConcurrentHashMap<String, String>();

        String validKey = createString(10);
        properties.put("@" + validKey + " ", VALID_VALUE_PROPERTY);
        Sanitizer.sanitizeProperties(properties);

        assertEquals(properties.size(), 1);
        Map.Entry<String, String> entry = properties.entrySet().iterator().next();
        assertEquals(entry.getKey(), validKey);
        assertEquals(entry.getValue(), VALID_VALUE_PROPERTY);
    }

    @Test
    public void testSanitizeNullMeasurements() throws Exception {
        Sanitizer.sanitizeMeasurements(null);
    }

    @Test
    public void testSanitizeEmptyMeasurements() throws Exception {
        ConcurrentMap<String, Double> measurements = new ConcurrentHashMap<String, Double>();
        Sanitizer.sanitizeMeasurements(measurements);

        assertTrue(measurements.isEmpty());
    }

    @Test
    public void testSanitizeValidMeasurements() throws Exception {
        ConcurrentMap<String, Double> measurements = new ConcurrentHashMap<String, Double>();
        measurements.put(VALID_KEY_PROPERTY, VALID_VALUE_MEASUREMENT);
        Sanitizer.sanitizeMeasurements(measurements);

        assertEquals(measurements.size(), 1);
        assertEquals(measurements.get(VALID_KEY_PROPERTY), VALID_VALUE_MEASUREMENT);
    }

    @Test
    public void testSanitizeIllegalCharsMeasurements() throws Exception {
        ConcurrentMap<String, Double> measurements = new ConcurrentHashMap<String, Double>();

        String validKey = createString(10);
        measurements.put("@" + validKey + " ", VALID_VALUE_MEASUREMENT);
        Sanitizer.sanitizeMeasurements(measurements);

        assertEquals(measurements.size(), 1);
        Map.Entry<String, Double> entry = measurements.entrySet().iterator().next();
        assertEquals(entry.getKey(), validKey);
        assertEquals(entry.getValue(), VALID_VALUE_MEASUREMENT);
    }

    @Test
    public void testSanitizeNullUri() throws Exception {
        Sanitizer.sanitizeUri(null);
    }

    @Test
    public void testSanitizeNonValidUri() throws Exception {
        URI uri = Sanitizer.sanitizeUri(NON_VALID_URL);
        assertNull(uri);
    }

    @Test
    public void testSanitizeValidUri() throws Exception {
        URI uri = Sanitizer.sanitizeUri(VALID_URL);
        assertNotNull(uri);
    }

    @Test
    public void testSanitizeNullValue() {
        String value = Sanitizer.sanitizeValue(null);
        assertNull(value);
    }

    @Test
    public void testSanitizeEmptyValue() {
        String value = Sanitizer.sanitizeValue("");
        assertEquals(value, "");
    }

    @Test
    public void testSanitizeLongValue() throws Exception {
        String name = Sanitizer.sanitizeValue(createString(Sanitizer.MAX_VALUE_LENGTH + 1));
        assertNotNull(name);
        assertEquals(name.length(), Sanitizer.MAX_VALUE_LENGTH);
    }

    @Test
    public void testSanitizeValidValue() throws Exception {
        String name = Sanitizer.sanitizeValue(VALID_VALUE_PROPERTY);
        assertNotNull(name);
        assertEquals(name, VALID_VALUE_PROPERTY);
    }

    @Test
    public void testSanitizeNullName() {
        String name = Sanitizer.sanitizeName(null);
        assertNull(name);
    }

    @Test
    public void testSanitizeEmptyName() {
        String name = Sanitizer.sanitizeName("");
        assertEquals(name, "");
    }

    @Test
    public void testSanitizeLongName() throws Exception {
        String name = Sanitizer.sanitizeName(createString(Sanitizer.MAX_NAME_LENGTH + 1));
        assertNotNull(name);
        assertEquals(name.length(), Sanitizer.MAX_NAME_LENGTH);
    }

    @Test
    public void testSanitizeValidName() throws Exception {
        String name = Sanitizer.sanitizeName(VALID_KEY_PROPERTY);
        assertNotNull(name);
        assertEquals(name, VALID_KEY_PROPERTY);
    }

    @Test
    public void testSanitizeNullMessage() {
        String message = Sanitizer.sanitizeMessage(null);
        assertNull(message);
    }

    @Test
    public void testSanitizeEmptyMessage() {
        String message = Sanitizer.sanitizeMessage("");
        assertEquals(message, "");
    }

    @Test
    public void testSanitizeLongMessage() throws Exception {
        String name = Sanitizer.sanitizeMessage(createString(Sanitizer.MAX_MESSAGE_LENGTH + 1));
        assertNotNull(name);
        assertEquals(name.length(), Sanitizer.MAX_MESSAGE_LENGTH);
    }

    @Test
    public void testSanitizeValidMessage() throws Exception {
        String originMessage = createString(10);
        String message = Sanitizer.sanitizeMessage(originMessage);
        assertNotNull(message);
        assertEquals(message, originMessage);
    }

    @Test
    public void testSafeStringToUrlWithNull() throws Exception {
        URI url = Sanitizer.safeStringToUri(null);
        assertNull(url);
    }

    @Test
    public void testSafeStringToUrlWithEmpty() throws Exception {
        URI uri = Sanitizer.sanitizeUri("");
        assertNull(uri);
    }

    @Test
    public void testSafeStringToUrlWithValid() throws Exception {
        URI uri = Sanitizer.sanitizeUri(VALID_URL);
        assertNotNull(uri);
    }

    @Test
    public void testSafeStringToUrlWithNonValid() throws Exception {
        URI uri = Sanitizer.sanitizeUri(NON_VALID_URL);
        assertNull(uri);
    }

    private String createString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            sb.append('a');
        }

        return sb.toString();
    }
}
