package com.microsoft.applicationinsights.web.internal.correlation.tracecontext;

import org.junit.Assert;
import org.junit.Test;

public class TraceparentTests {

    @Test
    public void canCreateValidTraceParentWithDefaultConstructor() {
        Traceparent traceparent = new Traceparent();
        Assert.assertNotNull(traceparent.traceId);
        Assert.assertNotNull(traceparent.spanId);
        Assert.assertEquals(0, traceparent.version);
        Assert.assertNotNull(traceparent.traceFlags);
    }

    @Test
    public void testTraceParentUniqueness() {
        Traceparent t1 = new Traceparent();
        Traceparent t2 = new Traceparent();
        Assert.assertNotEquals(t1.traceId, t2.traceId);
        Assert.assertNotEquals(t1.spanId, t2.spanId);

        // version is always 0
        Assert.assertEquals(t1.version, t2.version);

        // flags are currently 0, may change in future
        Assert.assertEquals(t1.traceFlags, t2.traceFlags);

    }

    @Test
    public void canCreateTraceParentWithProvidedValues() {
        String traceId = Traceparent.randomHex(16);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(0, traceId, spanId, 0);
        Assert.assertEquals(traceId, t1.traceId);
        Assert.assertEquals(spanId, t1.spanId);
        Assert.assertEquals(0, t1.version);
        Assert.assertEquals(0, t1.traceFlags);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenCreatingTraceParentWithIllegalTraceId() {
        String invalidTraceId = Traceparent.randomHex(32);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(0, invalidTraceId, spanId, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenCreatingTraceParentWithIllegalSpanId() {
        String traceId = Traceparent.randomHex(16);
        String invalidSpanId = Traceparent.randomHex(16);
        Traceparent t1 = new Traceparent(0, traceId, invalidSpanId, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenVersionNumberIsOutOfRange() {
        String traceId = Traceparent.randomHex(16);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(256, traceId, spanId, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenVersionNumberIsOutOfLowerRange() {
        String traceId = Traceparent.randomHex(16);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(-1, traceId, spanId, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenFlagIsOutOfLowerRange() {
        String traceId = Traceparent.randomHex(16);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(0, traceId, spanId, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenFlagIsOutOfUpperRange() {
        String traceId = Traceparent.randomHex(16);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(0, traceId, spanId, 256);
    }

    @Test
    public void canCreateTraceParentFromString() {
        String traceId = Traceparent.randomHex(16);
        String spanId = Traceparent.randomHex(8);
        Traceparent t1 = new Traceparent(0, traceId, spanId, 0);

        Traceparent t2 = Traceparent.fromString(t1.toString());
        Assert.assertEquals(t1.version, t2.version);
        Assert.assertEquals(t1.traceId, t2.traceId);
        Assert.assertEquals(t1.spanId, t2.spanId);
        Assert.assertEquals(t1.traceFlags, t2.traceFlags);

        // memory reference should be different
        Assert.assertFalse(t1 == t2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenTryingToCreateWithMalformedTraceparentString() {
        String invalidTraceId = Traceparent.randomHex(32);
        String invalidSpanId = Traceparent.randomHex(16);
        String invalidTraceparent = String.format("%02x-%s-%s-%02x", 0, invalidTraceId,
            invalidSpanId, 0);

        Traceparent t1 = Traceparent.fromString(invalidTraceparent);
    }

    @Test
    public void returnsNullTraceParentWhenTryingToCreateFromEmptyString() {
        Traceparent t1 = Traceparent.fromString("");
        Assert.assertNull(t1);
    }
}
