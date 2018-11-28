package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

public class Tracestate {

    public final String reserved;

    public Tracestate(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("invalid spanId");
        }
        reserved = value;
    }

    @Override
    public String toString() {
        return reserved;
    }

    public static Tracestate fromString(String s) {
        return new Tracestate(s);
    }

}
