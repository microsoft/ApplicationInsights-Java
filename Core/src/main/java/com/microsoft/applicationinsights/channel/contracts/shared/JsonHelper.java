package com.microsoft.applicationinsights.channel.contracts.shared;

import com.microsoft.applicationinsights.channel.contracts.DataPoint;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the helper class to have the json to integer/double/string and ext
 * converting.
 */
public final class JsonHelper {
    private static final String[] controlCharacters;
    static {
        // per RFC 4627 (JSON specification) section 2.5; escape control characters U+0000 -> U+001F
        controlCharacters = new String[0x20];
        for(int i = 0; i < controlCharacters.length; i++) {
            controlCharacters[i] = String.format("\\u%04X", i);
        }

        // JSON accepts the following friendly escape replacements instead of unicode
        controlCharacters['\b'] = "\\b";
        controlCharacters['\f'] = "\\f";
        controlCharacters['\n'] = "\\n";
        controlCharacters['\r'] = "\\r";
        controlCharacters['\t'] = "\\t";
    }

    private static String escapeJSON(String input) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"");
        for(int i = 0; i < input.length(); i++) {
            char charIndex = input.charAt(i);
            if(charIndex < controlCharacters.length) {
                String replacement = controlCharacters[charIndex];
                if (replacement == null) {
                    builder.append(charIndex);
                } else {
                    builder.append(replacement);
                }
            } else if(charIndex== '\"') {
                builder.append("\\\"");
            } else if(charIndex== '\\') {
                builder.append("\\\\");
            } else if(charIndex== '\u2028') {
                // JavaScript interprets '\u2028' as newline
                builder.append("\\u2028");
            } else if(charIndex == '\u2029') {
                // JavaScript interprets '\u2029' as newline
                builder.append("\\u2029");
            } else {
                builder.append(charIndex);
            }
        }

        builder.append("\"");
        return builder.toString();
    }


    /**
     * prevent caller to construct this object.
     */
    private JsonHelper() {
    }

    /**
     * integer to string.
     * 
     * @param value value of the integer.
     * @return String to represent the integer
     */
    public static String convert(Integer value) {
        return Integer.toString(value);
    }

    /**
     * long to string.
     * 
     * @param value value of the integer.
     * @return String to represent the long
     */
    public static String convert(Long value) {
        return Long.toString(value);
    }

    /**
     * character to string.
     * 
     * @param value value of the character.
     * @return String to represent the character
     */
    public static String convert(char value) {
        return Character.toString(value);
    }

    /**
     * double to string.
     * 
     * @param value value of the double.
     * @return String to represent the double
     */
    public static String convert(Double value) {
        return Double.toString(value);
    }

    /**
     * boolean to string.
     * 
     * @param value value of the boolean.
     * @return String to represent the boolean
     */
    public static String convert(boolean value) {
        return Boolean.toString(value);
    }

    /**
     * String to JSON String
     * 
     * @param value value of the string
     * @return JSON string to represent the string input parameter.
     */
    public static String convert(String value) {
        if (value == null) {
            return "null";
        } else if (value.length() == 0) {
            return "\"\"";
        } else {
            return escapeJSON(value);
        }
    }

    /**
     * serialize the IJsonSerializable to writer
     *
     * @param writer Writer object
     * @param value IJsonSerializable object
     * @throws IOException
     */
    public static void writeJsonSerializable(Writer writer, IJsonSerializable value)
            throws IOException {
        if (value != null) {
            value.serialize(writer);
        }
    }

    /**
     * serialize the map object to writer
     *
     * @param writer Writer object
     * @param map Map object
     * @throws IOException
     */
    public static <T> void writeDictionary(Writer writer, Map<String, T> map) throws IOException {
        if (map == null || map.size() < 1) {
            writer.write("null");
        } else {
            Set<String> keys = map.keySet();
            Iterator<String> iterator = keys.iterator();

            // special first case to ensure we have contents and that
            if(iterator.hasNext()) {
                writer.write("{");
                String key = iterator.next();
                T item = map.get(key);

                writer.write(key);
                writer.write(":");
                writeItem(writer, item);

                while (iterator.hasNext()) {
                    key = iterator.next();

                    writer.write(",");
                    writer.write(key);
                    writer.write(":");

                    item = map.get(key);
                    writeItem(writer, item);
                }

                writer.write("}");
            }
        }
    }

    /**
     * serialize the List object to writer
     *
     * @param writer Writer object
     * @param list List object
     * @throws IOException
     */
    public static <T extends IJsonSerializable> void writeList(Writer writer, List<T> list) throws IOException {
        if (list == null || list.size() < 1) {
            writer.write("null");
        } else {
            Iterator<T> iterator = list.iterator();
            if(iterator.hasNext()) {
                writer.write("[");

                IJsonSerializable item = iterator.next();
                item.serialize(writer);

                while(iterator.hasNext()){
                    item = iterator.next();
                    writer.write(",");
                    item.serialize(writer);
                }

                writer.write("]");
            }
        }
    }

    /**
     * Emits a primitive item of unknown type to the writer
     * @param writer The writer to which this item will be emitted
     * @param item The item to write
     * @param <T> The type of the item
     * @throws IOException if no handler exists for the type
     */
    private static <T> void writeItem(Writer writer, T item) throws IOException {
        if (item instanceof String) {
            writer.write(JsonHelper.convert((String) item));
        } else if (item instanceof Double) {
            writer.write(JsonHelper.convert((Double) item));
        } else if (item instanceof Integer) {
            writer.write(JsonHelper.convert((Integer) item));
        } else if (item instanceof Long) {
            writer.write(JsonHelper.convert((Long) item));
        } else {
            throw new IOException("Cannot serialize: " + item.toString());
        }
    }
}
