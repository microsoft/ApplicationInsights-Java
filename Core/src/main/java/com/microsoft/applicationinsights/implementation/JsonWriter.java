package com.microsoft.applicationinsights.implementation;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.util.LocalStringsUtils;

public class JsonWriter implements com.microsoft.applicationinsights.datacontracts.JsonWriter {
    private final Writer textWriter;
    private final EmptyObjectDetector emptyObjectDetector;
    private boolean currentObjectHasProperties;

    public JsonWriter(Writer textWriter) {
        this.emptyObjectDetector = new EmptyObjectDetector();
        this.textWriter = textWriter;
    }

    @Override
    public void writeStartArray() throws IOException {
        textWriter.append('[');
    }

    @Override
    public void writeStartObject() throws IOException {
        textWriter.append('{');
        currentObjectHasProperties = false;
    }

    @Override
    public void writeEndArray() throws IOException {
        textWriter.append(']');
    }

    @Override
    public void writeEndObject() throws IOException {
        textWriter.append('}');
    }

    @Override
    public void writeComma() throws IOException {
        textWriter.append(',');
    }

    @Override
    public void writeProperty(String name, String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            writePropertyName(name);
            writeString(value);
        }
    }

    @Override
    public void writeProperty(String name, Boolean value) throws IOException {
        if (value != null) {
            writePropertyName(name);
            textWriter.write(value ? "true" : "false");
        }
    }

    @Override
    public void writeProperty(String name, Integer value) throws IOException {
        if (value != null) {
            writePropertyName(name);
            textWriter.write(String.format("%d", value));
        }
    }

    @Override
    public void writeProperty(String name, Double value) throws IOException {
        if (value != null) {
            writePropertyName(name);
            textWriter.write(value.toString());
        }
    }

    @Override
    public void writeProperty(String name, Date value) throws IOException {
        if (value != null) {
            writePropertyName(name);
            writeString(LocalStringsUtils.getDateFormatter().format(value));
        }
    }

    @Override
    public void writeMetricsProperty(String name, Map<String, Double> values) throws IOException {
        if (values != null && values.size() > 0) {
            writePropertyName(name);
            writeStartObject();
            for (Map.Entry<String, Double> item : values.entrySet()) {
                writeProperty(item.getKey(), item.getValue());
            }
            writeEndObject();
        }
    }

    @Override
    public void writeProperty(String name, Map<String, String> values) throws IOException {
        if (values != null && values.size() > 0) {
            writePropertyName(name);
            writeStartObject();
            for (Map.Entry<String, String> item : values.entrySet()) {
                if (item.getValue() == null) {
                    continue;
                }
                writeProperty(item.getKey(), item.getValue());
            }
            writeEndObject();
        }
    }

    @Override
    public void writeProperty(String name, JsonSerializable value) throws IOException {
        if (!isNullOrEmpty(value)) {
            writePropertyName(name);
            //value.serialize(this);
        }
    }

    private boolean isNullOrEmpty(JsonSerializable value) throws IOException {
        if (value == null) {
            return true;
        }

        emptyObjectDetector.setEmpty(true);
        //value.serialize(emptyObjectDetector);
        return emptyObjectDetector.isEmpty();
    }

    @Override
    public void writePropertyName(String name) throws IOException {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("name");
        }

        if (currentObjectHasProperties) {
            writeComma();
        } else {
            currentObjectHasProperties = true;
        }

        writeString(name);
        textWriter.append(':');
    }

    private void writeString(String value) throws IOException {
        textWriter.append('"');

        for (char c : value.toCharArray()) {
            switch(c) {
            case '\\':
                textWriter.write("\\\\");
                break;
            case '"':
                textWriter.write("\\\"");
                break;
            case '\n':
                textWriter.write("\\n");
                break;
            case '\b':
                textWriter.write("\\b");
                break;
            case '\f':
                textWriter.write("\\f");
                break;
            case '\r':
                textWriter.write("\\r");
                break;
            case '\t':
                textWriter.write("\\t");
                break;
            default:
                textWriter.append(c);
                break;
            }
        }

        textWriter.append('"');
    }

    @Override
    public void writeRawValue(Object value) throws IOException {
        textWriter.write(String.format("%s", value.toString()));
    }

    private class EmptyObjectDetector implements com.microsoft.applicationinsights.datacontracts.JsonWriter {
        private boolean isEmpty;

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean isEmpty) {
            this.isEmpty = isEmpty;
        }

        @Override
        public void writeStartArray() {
        }

        @Override
        public void writeStartObject() {
        }

        @Override
        public void writeEndArray() {
        }

        @Override
        public void writeEndObject() {
        }

        @Override
        public void writeComma() {
        }

        @Override
        public void writeProperty(String name, String value) {
            if (value != null && !value.isEmpty())
                isEmpty = false;
        }

        @Override
        public void writeProperty(String name, Boolean value) {
            if (value != null) {
                this.isEmpty = false;
            }
        }

        @Override
        public void writeProperty(String name, Integer value) {
            if (value != null) {
                this.isEmpty = false;
            }
        }

        @Override
        public void writeProperty(String name, Double value) {
            if (value != null) {
                this.isEmpty = false;
            }
        }

        @Override
        public void writeProperty(String name, Date value) {
            if (value != null) {
                this.isEmpty = false;
            }
        }

        @Override
        public void writeMetricsProperty(String name, Map<String, Double> values) {
            if (values != null && values.size() > 0) {
                this.isEmpty = false;
            }
        }

        @Override
        public void writeProperty(String name, Map<String, String> values) {
            if (values != null && values.size() > 0) {
                this.isEmpty = false;
            }
        }

        @Override
        public void writeProperty(String name, JsonSerializable value) throws IOException {
            if (value != null) {
                //value.serialize(this);
            }
        }

        @Override
        public void writePropertyName(String name) {
        }

        @Override
        public void writeRawValue(Object value) {
            if (value != null) {
                this.isEmpty = false;
            }
        }
    }
}
