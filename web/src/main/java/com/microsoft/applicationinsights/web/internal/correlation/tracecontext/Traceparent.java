package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.ThreadLocalRandom;

public class Traceparent {

    @VisibleForTesting final int version;
    @VisibleForTesting final String traceId;
    @VisibleForTesting final String spanId;
    @VisibleForTesting final int traceFlags;

    private Traceparent(int version, String traceId, String spanId, int traceFlags, boolean check) {
        if (check) {
            validate(version, traceId, spanId, traceFlags);
        }
        this.version = version;
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceFlags = traceFlags;
    }

    public Traceparent(int version, String traceId, String spanId, int traceFlags) {
        this(version, traceId != null ? traceId : randomHex(16),
            spanId != null ? spanId : randomHex(8),
            traceFlags, true);
    }

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


    @Override
    public String toString() {
        return String.format("%02x-%s-%s-%02x", version, traceId, spanId, traceFlags);
    }

    @VisibleForTesting static String randomHex(int n) {
        byte[] bytes = new byte[n];
        ThreadLocalRandom.current().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

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

    public static Traceparent fromString(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        String[] arr = s.split("-");
        if (arr.length != 4) {
            return null;
        }
        if (arr[0].length() != 2) {
            return null;
        }
        if (arr[3].length() != 2) {
            return null;
        }

        return new Traceparent(
            (Character.digit(arr[0].charAt(0), 16) << 4) + Character.digit(arr[0].charAt(1), 16),
            arr[1],
            arr[2],
            (Character.digit(arr[3].charAt(0), 16) << 4) + Character.digit(arr[3].charAt(1), 16));
    }

}
