package com.microsoft.applicationinsights.internal.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Strings;

/**
 * Created by gupele on 1/7/2015.
 */
public final class Sanitizer {
    public final static int MAX_MAP_NAME_LENGTH = 150;
    public final static int MAX_VALUE_LENGTH = 1024;
    public final static int MAX_NAME_LENGTH = 1024;
    public final static int MAX_MESSAGE_LENGTH = 32768;
    public final static int MAX_URL_LENGTH = 2048;

    private final static String INVALID_NAME_CHARACTERS = "[^0-9a-zA-Z-._()\\/ ]";

    public static void sanitizeProperties(Map<String, String> map) {
        if (map != null) {
            HashMap<String, String> tempMap = new HashMap<String, String>(map.size());

            for (Map.Entry<String, String> entry : map.entrySet()) {
                String sanitizedKey = sanitizeKey(entry.getKey(), tempMap);
                String sanitizedValue = sanitizeValue(entry.getValue());
                tempMap.put(sanitizedKey, sanitizedValue);
            }

            map.clear();
            map.putAll(tempMap);
        }
    }

    public static void sanitizeMeasurements(Map<String, Double> map) {
        if (map != null) {
            HashMap<String, Double> tempMap = new HashMap<String, Double>(map.size());

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                String sanitizedKey = sanitizeKey(entry.getKey(), tempMap);
                tempMap.put(sanitizedKey, entry.getValue());
            }

            map.clear();
            map.putAll(tempMap);
        }
    }

    public static URI sanitizeUri(String urlAsString) {
        if (!Strings.isNullOrEmpty(urlAsString)) {

            if (urlAsString.length() > MAX_URL_LENGTH) {
                urlAsString = urlAsString.substring(0, MAX_URL_LENGTH);
            }

            // In case that the truncated string is invalid
            // URI we will not do nothing and let the Endpoint to drop the property
            URI temp = null;
            try {
                temp = new URI(urlAsString);
                return temp;
            } catch (Exception e) {
                // Swallow the exception
            }
        }

        return null;
    }

    public static String sanitizeValue(String value) {
        return trimAndTruncate(value, MAX_VALUE_LENGTH);
    }

    public static String sanitizeName(String name) {
        return trimAndTruncate(name, MAX_NAME_LENGTH);
    }

    public static String sanitizeMessage(String message) {
        return trimAndTruncate(message, MAX_MESSAGE_LENGTH);
    }

    public static boolean isUUID(String possibleUUID) {
        try {
            UUID.fromString(possibleUUID);
            return true;
        } catch (Exception e) {
        }

        return false;
    }

    public static URI safeStringToUri(String url) {
        if (Strings.isNullOrEmpty(url)) {
            return null;
        }

        URI result = null;
        try {
            result = new URI(url);
        } catch (Exception e) {
        }

        return result;
    }

    private static <V> String sanitizeKey(String key, Map<String, V> map) {
        String sanitizedKey = trimAndTruncate(key, MAX_MAP_NAME_LENGTH);

        sanitizedKey = sanitizedKey.replaceAll(INVALID_NAME_CHARACTERS, "");
        sanitizedKey = MakeKeyNonEmpty(sanitizedKey);
        sanitizedKey = MakeKeyUnique(sanitizedKey, map);
        return sanitizedKey;
    }

    private static String trimAndTruncate(String value, int maxLength) {
        if (value == null) {
            return value;
        }

        String sanitized = value.trim();
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }

        return sanitized;
    }

    private static String MakeKeyNonEmpty(String key) {
        return Strings.isNullOrEmpty(key) ? "(required property name is empty)" : key;
    }

    private static <V> String MakeKeyUnique(String key, Map<String, V> map)
    {
        if (map.containsKey(key)) {
            int uniqueNumberLength = 3;
            String truncatedKey = truncate(key, MAX_MAP_NAME_LENGTH - uniqueNumberLength);
            int candidate = 1;
            do {
                key = truncatedKey + String.format("%0%dd", uniqueNumberLength, candidate);
                candidate++;
            }
            while (map.containsKey(key));
        }

        return key;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
