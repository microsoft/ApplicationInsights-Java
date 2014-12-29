package com.microsoft.applicationinsights.datacontracts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

import com.microsoft.applicationinsights.util.LocalStringsUtils;

/**
 * Created by gupele on 12/24/2014.
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

    public void write(String name, int value) throws IOException {
        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Integer value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, double value) throws IOException {
        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Double value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, short value) throws IOException {
        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Short value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, long value) throws IOException {
        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Long value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, boolean value) throws IOException {
        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
        separator = JSON_SEPARATOR;
    }

    public void write(String name, Boolean value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(JSON_COMMA);
        out.write(String.valueOf(value));
        out.write(JSON_COMMA);
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

    public void write(String name, String value) throws IOException {
        if (value == null) {
            return;
        }

        writeName(name);
        out.write(JSON_COMMA);
        out.write(value);
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
                    writeName(entry.getKey());
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
            out.write(JSON_COMMA);
            out.write(String.valueOf(item));
            out.write(JSON_COMMA);
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
}
