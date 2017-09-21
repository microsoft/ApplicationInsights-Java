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

package com.microsoft.applicationinsights.telemetry;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.StringCharacterIterator;
import java.util.*;

/**
 * This class knows how to transform data that is relevant to {@link Telemetry} instances into JSON.
 */
public final class JsonTelemetryDataSerializer {

    private final static String JSON_SEPARATOR = ",";
    private final static String JSON_START_OBJECT = "{";
    private final static String JSON_CLOSE_OBJECT = "}";
    private final static String JSON_START_ARRAY = "[";
    private final static String JSON_CLOSE_ARRAY = "]";
    private final static String JSON_COMMA = "\"";
    private final static String JSON_NAME_VALUE_SEPARATOR = ":";
    private final static String JSON_EMPTY_OBJECT = "{}";

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    private Writer out;

    private String separator = "";

    public JsonTelemetryDataSerializer(Writer out) throws IOException {
        reset(out);
    }

    public void reset(Writer out) throws IOException {
        separator = "";
        this.out = out;
        this.out.write(JSON_START_OBJECT);
    }

    public void close() throws IOException {
        out.write(JSON_CLOSE_OBJECT);
        out.close();
    }

    public void write(String name, Duration value) throws IOException {
        writeName(name);
        write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, DataPointType value) throws IOException {
        if (value != null) {
            writeName(name);
            out.write(String.valueOf(value.getValue()));
            separator = JSON_SEPARATOR;
        }
    }

    public void write(String name, int value) throws IOException {
        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, com.microsoft.applicationinsights.internal.schemav2.SeverityLevel value) throws IOException {
        if (value != null) {
            writeName(name);
            out.write(String.valueOf(value));
            separator = JSON_SEPARATOR;
        }
    }

    public void write(String name, Integer value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, double value) throws IOException {
        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Double value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, short value) throws IOException {
        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Short value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, long value) throws IOException {
        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Long value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, boolean value) throws IOException {
        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Boolean value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Date value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        write(LocalStringsUtils.getDateFormatter().format(value));
        separator = JSON_SEPARATOR;
    }

    /**
     * This method is deprecated and is kept because there is still some dependency on it
     * which will be removed in coming versions
     * @deprecated
     * @param name
     * @param value
     * @throws IOException
     */
    @Deprecated
    public void write(String name, String value) throws IOException {
        //This method is practically not used anywhere .Will be removed with all other
        //obsolete classes in next major release
    }

    public void write(String name, String value, int len, boolean isReq) throws IOException {
        if (value == null && !isReq) {
            return;
        }

        //If field is required and not present set default value
        if (value == null && isReq) {
            value = "DEFAULT " + name;
        }

        writeName(name);
        out.write(JSON_COMMA);
        String sanitizedValue = sanitizeStringForJSON(value, len);
        out.write(sanitizedValue);
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public <T extends JsonSerializable> void write(String name, T value) throws IOException {
        if (value == null) {
            return;
        }

        String jsonStringToAppend = createJsonFor(value);
        if (Strings.isNullOrEmpty(jsonStringToAppend)) {
            return;
        }

        writeName(name);
        out.write(jsonStringToAppend);

        separator = JSON_SEPARATOR;
    }

    public <T> void write(String name, Map<String, T> map) throws IOException {

        if (map == null) {
            return;
        }

        writeName(name);
        try {
            if (map.size() < 1) {
                out.write("null");
            } else {
                out.write(JSON_START_OBJECT);

                separator = "";
                for (Map.Entry<String, T> entry : map.entrySet()) {
                    writeName(sanitizeKey(entry.getKey()));
                    write(entry.getValue());
                    separator = JSON_SEPARATOR;
                }

                out.write(JSON_CLOSE_OBJECT);
            }
        } finally {
            separator = JSON_SEPARATOR;
        }
    }


    public <T> void write(String name, List<T> list) throws IOException {
        if (list == null) {
            return;
        }

        writeName(name);
        try {
            if (list.size() < 1) {
                out.write("null");
            } else {
                out.write(JSON_START_ARRAY);
                separator = "";
                for (T item : list) {
                    out.write(separator);
                    write(item);
                    separator = JSON_SEPARATOR;
                }

                out.write(JSON_CLOSE_ARRAY);
            }
        } finally {
            separator = JSON_SEPARATOR;
        }
    }

    private <T> void write(T item) throws IOException {
        if (item instanceof JsonSerializable) {
            StringWriter stringWriter = new StringWriter();

            String jsonStringToAppend = createJsonFor((JsonSerializable)item);
            if (Strings.isNullOrEmpty(jsonStringToAppend)) {
                return;
            }

            out.write(jsonStringToAppend);
        } else {
            if (WRAPPER_TYPES.contains(item.getClass()))
            {
                out.write(String.valueOf(item));
            } else {
                String truncatedName = truncate(String.valueOf(item), 8192);
                String sanitizedItem = sanitizeStringForJSON(truncatedName, 8192);
                out.write(JSON_COMMA);
                out.write(sanitizedItem);
                out.write(JSON_COMMA);
            }
        }
    }

    private <T extends JsonSerializable> String createJsonFor(T value) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer temp = new JsonTelemetryDataSerializer(new BufferedWriter(stringWriter));

        value.serialize(temp);
        temp.close();
        String jsonStringToAppend = stringWriter.toString();
        if (Strings.isNullOrEmpty(jsonStringToAppend) || JSON_EMPTY_OBJECT.equals(jsonStringToAppend)) {
            return "";
        }

        return jsonStringToAppend;
    }

    private void writeName(String name) throws IOException {

        out.write(separator);
        out.write(JSON_COMMA);
        out.write(name);
        out.write(JSON_COMMA);
        out.write(JSON_NAME_VALUE_SEPARATOR);
    }

    private static Set<Class<?>> getWrapperTypes()
    {
        Set<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        return ret;
    }

    private String sanitizeStringForJSON(String text, int maxLength) {

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

    private String sanitizeKey(String key) {
        String sanitizedKey = trimAndTruncate(key, 150);
        sanitizedKey = sanitizeStringForJSON(sanitizedKey, 150);
        sanitizedKey = MakeKeyNonEmpty(sanitizedKey);
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

    private String MakeKeyNonEmpty(String key) {
        return Strings.isNullOrEmpty(key) ? "(required property name is empty)" : key;
    }

    private String truncate(String value, int len) {
        if (value.length() > len) {
            return value.substring(0, len);
        }
        return value;
    }

}
