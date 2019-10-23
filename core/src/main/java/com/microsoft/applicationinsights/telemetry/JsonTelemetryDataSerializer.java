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
import java.io.Writer;
import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

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
    private final static int DELTA = 2;

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
        out.flush();
        // do not close the underlying writer (e.g. in case writing multiple telemetry to gzip output stream)
    }

    public void write(String name, Duration value) throws IOException {
        writeName(name);
        out.write(JSON_COMMA);
        value.toString(out);
        out.write(JSON_COMMA);
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
            out.write(JSON_COMMA);
            out.write(String.valueOf(value));
            out.write(JSON_COMMA);
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
        out.write(Double.isNaN(value) || Double.isInfinite(value) ? "0.0" : String.valueOf(value));
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Double value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(Double.isNaN(value) || Double.isInfinite(value) ? "0.0" : String.valueOf(value));
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
        //obsolete classes in next major unbindFromRunawayChildThreads
    }

    public void write(String name, String value, int len) throws IOException {
        if (value == null || value.equals("")) {
            return;
        }
        writeToJson(name, value, len);
    }

    public void writeRequired(String name, String value, int len) throws IOException{
        //If field is required and not present set default value
        if (value == null || value.equals("")) {
            value = "DEFAULT " + name;
        }
        writeToJson(name, value, len);
    }

    private void writeToJson(String name, String value, int len) throws IOException {

        writeName(name);
        out.write(JSON_COMMA);
        sanitizeStringForJSON(out, value, len);
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public <T extends JsonSerializable> void write(String name, T value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        writeObject(value);

        separator = JSON_SEPARATOR;
    }

    public <T> void write(String name, Map<String, T> map) throws IOException {

        if (map == null || map.isEmpty()) {
            return;
        }

        writeName(name);
        try {
            out.write(JSON_START_OBJECT);

            separator = "";
            for (Map.Entry<String, T> entry : map.entrySet()) {
                out.write(separator);
                out.write(JSON_COMMA);
                sanitizeKey(out, entry.getKey());
                out.write(JSON_COMMA);
                out.write(JSON_NAME_VALUE_SEPARATOR);
                write(entry.getValue());
                separator = JSON_SEPARATOR;
            }

            out.write(JSON_CLOSE_OBJECT);
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
            writeObject((JsonSerializable) item);
        } else {
            if (WRAPPER_TYPES.contains(item.getClass()))
            {
                out.write(String.valueOf(item));
            } else {
                String truncatedName = truncate(String.valueOf(item), 8192);
                out.write(JSON_COMMA);
                sanitizeStringForJSON(out, truncatedName, 8192);
                out.write(JSON_COMMA);
            }
        }
    }

    private void writeObject(JsonSerializable value) throws IOException {
        String savedSeparator = separator;
        separator = "";
        try {
            out.write(JSON_START_OBJECT);
            value.serialize(this);
            out.write(JSON_CLOSE_OBJECT);
        } finally {
            separator = savedSeparator;
        }
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

    private void sanitizeStringForJSON(Appendable out, String text, int maxLength) throws IOException {

        int count = 0;
        StringCharacterIterator iterator = new StringCharacterIterator(text);

        for (char curr = iterator.current(); curr != iterator.DONE && count < maxLength - DELTA; curr = iterator.next()) {
            if( curr == '\"' ){
                out.append("\\\"");
                count += 2;
            }
            else if(curr == '\\'){
                out.append("\\\\");
                count += 2;
            }
            else if(curr == '\b'){
                out.append("\\b");
                count += 2;
            }
            else if(curr == '\f'){
                out.append("\\f");
                count += 2;
            }
            else if(curr == '\n'){
                out.append("\\n");
                count += 2;
            }
            else if(curr == '\r'){
                out.append("\\r");
                count += 2;
            }
            else if(curr == '\t'){
                out.append("\\t");
                count += 2;
            }
            else if (!Character.isISOControl(curr)){
                out.append(curr);
                count++;
            }
            else {
                if (count + 7 < maxLength) { // needs 7 more character space to be appended
                    out.append("\\u");
                    out.append((String.format( "%04x", Integer.valueOf(curr))));
                    count += 7;
                }
                else {
                    break;
                }
            }
        }
    }

    private void sanitizeKey(Appendable out, String key) throws IOException {
        String trimmed = trimAndTruncate(key, 150);
        if (Strings.isNullOrEmpty(trimmed)) {
            trimmed = "(required property name is empty)";
        }
        sanitizeStringForJSON(out, trimmed, 150);
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
