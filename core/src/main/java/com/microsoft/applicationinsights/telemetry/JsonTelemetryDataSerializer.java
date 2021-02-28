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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.squareup.moshi.JsonWriter;

/**
 * This class knows how to transform data that is relevant to {@link Telemetry} instances into JSON.
 */
public final class JsonTelemetryDataSerializer {

    private JsonWriter out;

    public JsonTelemetryDataSerializer(JsonWriter out) throws IOException {
        reset(out);
    }

    public void reset(JsonWriter out) throws IOException {
        this.out = out;
        this.out.beginObject();
    }

    public void close() throws IOException {
        out.endObject();
        // do not flush or close the underlying writer (e.g. in case writing multiple telemetry to gzip output stream)
    }

    public void write(String name, Duration value) throws IOException {
        writeName(name);
        out.value(value.toString());
    }

    public void write(String name, DataPointType value) throws IOException {
        if (value != null) {
            writeName(name);
            out.value(value.getValue());
        }
    }

    public void write(String name, int value) throws IOException {
        writeName(name);
        out.value(value);
    }

    public void write(String name, com.microsoft.applicationinsights.internal.schemav2.SeverityLevel value)
            throws IOException {
        if (value != null) {
            writeName(name);
            out.value(String.valueOf(value));
        }
    }

    public void write(String name, Integer value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.value(value);
    }

    public void write(String name, double value) throws IOException {
        writeName(name);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            out.value(0);
        } else {
            out.value(value);
        }
    }

    public void write(String name, Double value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            out.value(0);
        } else {
            out.value(value);
        }
    }

    public void write(String name, short value) throws IOException {
        writeName(name);
        out.value(value);
    }

    public void write(String name, long value) throws IOException {
        writeName(name);
        out.value(value);
    }

    public void write(String name, Long value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.value(value);
    }

    public void write(String name, boolean value) throws IOException {
        writeName(name);
        out.value(value);
    }

    public void write(String name, Boolean value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.value(value);
    }

    public void write(String name, String value, int len) throws IOException {
        if (value == null || value.equals("")) {
            return;
        }
        writeToJson(name, value, len);
    }

    public void writeRequired(String name, String value, int len) throws IOException {
        //If field is required and not present set default value
        if (value == null || value.equals("")) {
            value = "DEFAULT " + name;
        }
        writeToJson(name, value, len);
    }

    private void writeToJson(String name, String value, int len) throws IOException {

        writeName(name);
        sanitizeValue(out, value, len);
    }

    public <T extends JsonSerializable> void write(String name, T value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        writeObject(value);
    }

    public <T> void write(String name, Map<String, T> map) throws IOException {

        if (map == null || map.isEmpty()) {
            return;
        }

        writeName(name);
        out.beginObject();

        for (Map.Entry<String, T> entry : map.entrySet()) {
            sanitizeKey(out, entry.getKey());
            write(entry.getValue());
        }

        out.endObject();
    }


    public <T> void write(String name, List<T> list) throws IOException {
        if (list == null) {
            return;
        }

        writeName(name);
        if (list.size() < 1) {
            out.nullValue();
        } else {
            out.beginArray();
            for (T item : list) {
                write(item);
            }
            out.endArray();
        }
    }

    private <T> void write(T item) throws IOException {
        if (item instanceof JsonSerializable) {
            writeObject((JsonSerializable) item);
        } else if (item instanceof Number) {
            out.value((Number) item);
        } else if (item instanceof Boolean) {
            out.value((Boolean) item);
        } else if (item instanceof Character) {
            out.value((Character) item);
        } else {
            String truncatedName = truncate(String.valueOf(item), 8192);
            sanitizeValue(out, truncatedName, 8192);
        }
    }

    private void writeObject(JsonSerializable value) throws IOException {
        out.beginObject();
        value.serialize(this);
        out.endObject();
    }

    private void writeName(String name) throws IOException {
        out.name(name);
    }

    private void sanitizeValue(JsonWriter out, String text, int maxLength) throws IOException {
        if (text.length() <= maxLength) {
            out.value(text);
        } else {
            out.value(text.substring(0, maxLength));
        }
    }

    private void sanitizeKey(JsonWriter out, String key) throws IOException {
        String trimmed = trimAndTruncate(key, 150);
        if (Strings.isNullOrEmpty(trimmed)) {
            trimmed = "(required property name is empty)";
        }
        sanitizeName(out, trimmed, 150);
    }

    private void sanitizeName(JsonWriter out, String text, int maxLength) throws IOException {
        if (text.length() <= maxLength) {
            out.name(text);
        } else {
            out.name(text.substring(0, maxLength));
        }
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

    private String truncate(String value, int len) {
        if (value.length() > len) {
            return value.substring(0, len);
        }
        return value;
    }
}
