package com.microsoft.applicationinsights.agent.internal;

import org.junit.*;

import static com.microsoft.applicationinsights.agent.internal.SamplingPercentage.roundToNearest;
import static com.microsoft.applicationinsights.agent.internal.SamplingPercentage.significantlyRounded;
import static org.junit.Assert.*;

public class SamplingPercentageTest {

    @Test
    public void testRoundToNearest() {

        // perfect
        assertEquals(100, roundToNearest(100), 0);
        assertEquals(50, roundToNearest(50), 0);
        assertEquals(10, roundToNearest(10), 0);
        assertEquals(2, roundToNearest(2), 0);
        assertEquals(0.1, roundToNearest(0.1), 0.001);
        assertEquals(0.001, roundToNearest(0.001), 0.00001);
        assertEquals(0, roundToNearest(0), 0);

        // imperfect
        assertEquals(100, roundToNearest(90), 0);
        assertEquals(50, roundToNearest(51), 0);
        assertEquals(50, roundToNearest(49), 0);
        assertEquals(33.333, roundToNearest(34), 0.01);
        assertEquals(33.333, roundToNearest(33), 0.01);
        assertEquals(25, roundToNearest(26), 0);
        assertEquals(25, roundToNearest(24), 0);
    }

    @Test
    public void testSignificantlyRounded() {

        assertTrue(significantlyRounded(99, 100));
        assertTrue(significantlyRounded(51, 50));
        assertTrue(significantlyRounded(49, 50));
        assertTrue(significantlyRounded(35, 100.0/3));
        assertTrue(significantlyRounded(32, 100.0/3));

        assertFalse(significantlyRounded(100, 100));
        assertFalse(significantlyRounded(50, 50));
        assertFalse(significantlyRounded(34, 100.0/3));
        assertFalse(significantlyRounded(33.3, 100.0/3));
        assertFalse(significantlyRounded(33, 100.0/3));
        assertFalse(significantlyRounded(1, 1));
        assertFalse(significantlyRounded(0, 0));
    }
}
