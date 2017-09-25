/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.util;

import java.net.URI;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Strings;
import org.apache.http.annotation.Obsolete;

/**
 * Created by gupele on 1/7/2015.
 *
 * Most of the methods of this class are now obsolete except URL methods which will
 * be moved soon.
 */
public final class Sanitizer {
    public final static int MAX_MAP_NAME_LENGTH = 150;

    // Schema V2 allows max length to be 8192
    public final static int MAX_VALUE_LENGTH = 8192;

    public final static int MAX_NAME_LENGTH = 1024;
    public final static int MAX_MESSAGE_LENGTH = 32768;
    public final static int MAX_URL_LENGTH = 2048;

    private final static String INVALID_NAME_CHARACTERS = "[^0-9a-zA-Z-._()\\/ ]";

    @Obsolete
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

    @Obsolete
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

    @Obsolete
    public static String sanitizeValue(String value) {
        String truncatedString = trimAndTruncate(value, MAX_VALUE_LENGTH);
        String sanitizedString = sanitizeStringForJSON(truncatedString, MAX_VALUE_LENGTH);
        return sanitizedString;
    }

    @Obsolete
    public static String sanitizeName(String name) {
        return trimAndTruncate(name, MAX_NAME_LENGTH);
    }

    @Obsolete
    public static String sanitizeMessage(String message) {
        return trimAndTruncate(message, MAX_MESSAGE_LENGTH);
    }

    @Obsolete
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

    @Obsolete
    private static <V> String sanitizeKey(String key, Map<String, V> map) {
        String sanitizedKey = trimAndTruncate(key, MAX_MAP_NAME_LENGTH);
        sanitizedKey = sanitizeStringForJSON(sanitizedKey, MAX_MAP_NAME_LENGTH);
        sanitizedKey = MakeKeyNonEmpty(sanitizedKey);
        sanitizedKey = MakeKeyUnique(sanitizedKey, map);
        return sanitizedKey;
    }

    @Obsolete
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

    @Obsolete
    private static String MakeKeyNonEmpty(String key) {
        return Strings.isNullOrEmpty(key) ? "(required property name is empty)" : key;
    }

    @Obsolete
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

    @Obsolete
    private static String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Obsolete
    private static String sanitizeStringForJSON(String text, int maxLength) {

        final StringBuilder result = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(text);
        for (char curr = iterator.current(); curr != iterator.DONE && result.length() < maxLength - 2; curr = iterator.next()) {
            if( curr == '\"' ){
                result.append("\\\"");
            }
            else if (curr == '\'') {
                result.append("\\\'");
            }
            else if(curr == '\\'){
                result.append("\\\\");
            }
            else if(curr == '/'){
                result.append("\\/");
            }
            else if(curr == '\b'){
                result.append("\\b");
            }
            else if(curr == '\f'){
                result.append("\\f");
            }
            else if(curr == '\n'){
                result.append("\\n");
            }
            else if(curr == '\r'){
                result.append("\\r");
            }
            else if(curr == '\t'){
                result.append("\\t");
            }
            else if (!Character.isISOControl(curr)){
                result.append(curr);
            }
            else {
                if (result.length() + 7 < maxLength) { // needs 7 more character space to be appended
                    result.append("\\u");
                    result.append((String.format( "%04x", Integer.valueOf(curr))));
                }
                else {
                    break;
                }
            }
        }
        return result.toString();
    }
}
