package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

import org.junit.Assert;
import org.junit.Test;

public class TracestateTests {

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenTracestateIsNull() {
        new Tracestate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenTracestateIsEmpty() {
        new Tracestate("");
    }

    @Test
    public void canCreateTraceStateWithString() {
        String tracestate = "az=cid-v1:120";
        Tracestate t1 = new Tracestate(tracestate);
        Assert.assertEquals(tracestate, t1.toString());
    }
}
