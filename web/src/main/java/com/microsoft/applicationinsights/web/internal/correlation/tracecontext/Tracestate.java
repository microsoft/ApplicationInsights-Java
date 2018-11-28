package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

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
@Experimental
public class Tracestate {

    /**
     * Field to capture tracestate header
     */
    public final String reserved;

    /**
     * Ctor that creates tracestate object from given value
     */
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

    /**
     * Converts Tracestate header to Object representation
     *
     * @return Tracestate
     */
    public static Tracestate fromString(String s) {
        return new Tracestate(s);
    }

}
