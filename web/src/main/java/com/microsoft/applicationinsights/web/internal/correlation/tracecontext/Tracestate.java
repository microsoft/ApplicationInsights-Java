package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.annotation.Experimental;

/**
 * Class that represents Tracestate header based on
 *
 * @author Reiley Yang
 * @author Dhaval Doshi
 * @link https://github.com/w3c/trace-context/blob/master/trace_context/HTTP_HEADER_FORMAT.md
 *
 * Implementations can add vendor specific details here.
 */
public class Tracestate {

    /**
     * Internal representation of the tracestate
     */
    private LinkedHashMap<String, String> internalList = new LinkedHashMap<>(32);

    /**
     * String representation of the tracestate
     */
    private String internalString = null;

    private static String KEY_WITHOUT_VENDOR_FORMAT = "[a-z][_0-9a-z\\-\\*\\/]{0,255}";
    private static String KEY_WITH_VENDOR_FORMAT = "[a-z][_0-9a-z\\-\\*\\/]{0,240}@[a-z][_0-9a-z\\-\\*\\/]{0,13}";
    private static String KEY_FORMAT = KEY_WITHOUT_VENDOR_FORMAT + "|" + KEY_WITH_VENDOR_FORMAT;
    private static String VALUE_FORMAT = "[\\x20-\\x2b\\x2d-\\x3c\\x3e-\\x7e]{0,255}[\\x21-\\x2b\\x2d-\\x3c\\x3e-\\x7e]";

    private static Pattern KEY_VALIDATION_RE = Pattern.compile("^" + KEY_FORMAT + "$");
    private static Pattern VALUE_VALIDATION_RE = Pattern.compile("^" + VALUE_FORMAT + "$");

    private static String DELIMITER_FORMAT = "[ \\t]*,[ \\t]*";
    private static String MEMBER_FORMAT = String.format("(%s)(=)(%s)", KEY_FORMAT, VALUE_FORMAT);

    private static Pattern DELIMITER_FORMAT_RE = Pattern.compile(DELIMITER_FORMAT);
    private static Pattern MEMBER_FORMAT_RE = Pattern.compile("^" + MEMBER_FORMAT + "$");

    /**
     * Ctor that creates tracestate object from given value
     */
    public Tracestate(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        
        String[] values = DELIMITER_FORMAT_RE.split(input);
        for (String item : values) {
            Matcher m = MEMBER_FORMAT_RE.matcher(item);
            if (!m.find()) {
                throw new IllegalArgumentException(String.format("invalid string %s in tracestate", item));
            }
            String key = m.group(1);
            String value = m.group(3);
            if (internalList.get(key) != null) {
                throw new IllegalArgumentException(String.format("duplicated keys %s in tracestate", key));
            }
            internalList.put(key, value);
        }
        if (internalList.size() > 32) {
            throw new IllegalArgumentException("cannot have more than 32 key-value pairs");
        }
        internalString = toInternalString();
    }

    /**
     * Ctor that creates a tracestate object from a parent one
     */
    public Tracestate(Tracestate parent, String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (!KEY_VALIDATION_RE.matcher(key).find()) {
            throw new IllegalArgumentException("invalid key format");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!VALUE_VALIDATION_RE.matcher(value).find()) {
            throw new IllegalArgumentException("invalid value format");
        }
        internalList.put(key, value);
        if (parent != null) {
            for (String k : parent.internalList.keySet()) {
                internalList.put(k, parent.internalList.get(k));
            }
            internalList.put(key, value);
        }
        internalString = toInternalString();
    }

    public String get(String key) {
        return internalList.get(key);
    }

    /**
     * Converts the Tracestate object to header format
     *
     * @return tracestate
     */
    @Override
    public String toString() {
        return internalString;
    }

    /**
     * Converts Tracestate header to Object representation
     *
     * @return Tracestate
     */
    public static Tracestate fromString(String s) {
        return new Tracestate(s);
    }

    private String toInternalString() {
        boolean isFirst = true;
        StringBuilder stringBuilder = new StringBuilder(512);
        for (String key : internalList.keySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                stringBuilder.append(",");
            }
            stringBuilder.append(key);
            stringBuilder.append("=");
            stringBuilder.append(internalList.get(key));
        }
        return stringBuilder.toString();
    }

}
