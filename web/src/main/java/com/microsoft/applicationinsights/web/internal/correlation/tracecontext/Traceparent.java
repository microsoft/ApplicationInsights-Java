package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

import java.util.concurrent.ThreadLocalRandom;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.annotation.Experimental;

/**
 * This class represents the Traceparent data structure based on
 *
 * @author Reily Yang
 * @author Dhaval Doshi
 * @link https://github.com/w3c/trace-context/blob/master/trace_context/HTTP_HEADER_FORMAT.md
 */
@Experimental
public class Traceparent {

    /**
     * Version number between range [0,255] inclusive
     */
    @VisibleForTesting
    final int version;

    /**
     * 16 byte trace-id that is used to uniquely identify a distributed trace
     */
    @VisibleForTesting
    final String traceId;

    /**
     * It is a 8 byte ID that represents the caller span
     */
    @VisibleForTesting
    final String spanId;

    /**
     * An 8-bit field that controls tracing flags such as sampling, trace level etc.
     */
    @VisibleForTesting
    final int traceFlags;

    private Traceparent(int version, String traceId, String spanId, int traceFlags, boolean check) {
        if (check) {
            validate(version, traceId, spanId, traceFlags);
        }
        this.version = version;
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceFlags = traceFlags;
    }

    /**
     * The constructor that tries to create Traceparent Object from given version, traceId, spanID
     * and traceFlags.
     */
    public Traceparent(int version, String traceId, String spanId, int traceFlags) {
        this(version, traceId != null ? traceId : randomHex(16),
            spanId != null ? spanId : randomHex(8),
            traceFlags, true);
    }

    /**
     * This constructor creates a new Traceparent object having new traceId. It should only be used
     * if the call is the starting point of distributed trace.
     */
    public Traceparent() {
        this(0, randomHex(16), randomHex(8), 0, false);
    }

    public String getTraceId() {
        return traceId;
    }

    public int getTraceFlags() {
        return traceFlags;
    }

    public String getSpanId() {
        return spanId;
    }

    /**
     * Validates the given input based on W3C specifications.
     */
    private static void validate(int version, String traceId, String spanId, int traceFlags)
        throws IllegalArgumentException {
        if (version < 0 || version > 254) {
            throw new IllegalArgumentException("version must be within range [0, 255)");
        }
        if (!isHex(traceId, 32)) {
            throw new IllegalArgumentException("invalid traceId");
        }
        if (traceId.equals("00000000000000000000000000000000")) {
            throw new IllegalArgumentException("invalid traceId");
        }
        if (!isHex(spanId, 16)) {
            throw new IllegalArgumentException("invalid spanId");
        }
        if (spanId.equals("0000000000000000")) {
            throw new IllegalArgumentException("invalid spanId");
        }
        if (traceFlags < 0 || traceFlags > 255) {
            throw new IllegalArgumentException("traceFlags must be within range [0, 255]");
        }
    }

    /**
     * Converts the Traceparent object to header format Eg: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
     *
     * @return traceparent
     */
    @Override
    public String toString() {
        return String.format("%02x-%s-%s-%02x", version, traceId, spanId, traceFlags);
    }

    /**
     * Helper method to create a random hexadecimal string of n bytes.
     *
     * @return n byte hexadecimal string
     */
    @VisibleForTesting
    static String randomHex(int n) {
        byte[] bytes = new byte[n];
        ThreadLocalRandom.current().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Helper method to check if a given string of n bytes is hexadecimal
     *
     * @return boolean
     */
    private static boolean isHex(String s, int n) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (s.length() != n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '9') {
                continue;
            }
            if ('a' <= c && c <= 'f') {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * converts traceparent from String to Traceparent object
     *
     * @return Traceparent
     */
    public static Traceparent fromString(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        String[] arr = s.split("-");
        if (arr.length != 4) {
            return null;
        }
        if (!isHex(arr[0], 2)) {
            return null;
        }
        if (!isHex(arr[3], 2)) {
            return null;
        }

        return new Traceparent(
            (Character.digit(arr[0].charAt(0), 16) << 4) + Character.digit(arr[0].charAt(1), 16),
            arr[1],
            arr[2],
            (Character.digit(arr[3].charAt(0), 16) << 4) + Character.digit(arr[3].charAt(1), 16));
    }

}
