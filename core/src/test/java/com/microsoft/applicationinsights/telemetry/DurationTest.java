/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.telemetry;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DurationTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNovValidNegativeHours() {
        new Duration(0, -24, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidPositiveHours() {
        new Duration(0, 24, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidNegativeMinutes() {
        new Duration(0, 0, -60, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidPositiveMinutes() {
        new Duration(0, 0, -60, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidNegativeSeconds() {
        new Duration(0, 0, 0, -60, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidPositiveSeconds() {
        new Duration(0, 0, 0, 60, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidPositiveMilliseconds() {
        new Duration(0, 0, 0, 0, 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNovValidNegativeMilliseconds() {
        new Duration(0, 0, 0, 0, -1);
    }

    @Test
    public void testZero() {
        Duration duration = new Duration(0);

        verify(duration, 0, 0, 0, 0, 0, "00:00:00");
    }

    @Test
    public void testZeroWithAllArgs() {
        Duration duration = new Duration(0, 0, 0, 0, 0);

        verify(duration, 0, 0, 0, 0, 0, "00:00:00");
    }

    @Test
    public void testMinusFourteenDays() {
        Duration duration = new Duration(-14, 0, 0, 0, 0);

        verify(duration, -14, 0, 0, 0, 0, "-14.00:00:00");
    }

    @Test
    public void testMinNegativeDays() {
        Duration duration = new Duration(Integer.MIN_VALUE, 0, 0, 0, 0);

        String minValue = String.valueOf(Integer.MIN_VALUE);
        verify(duration, Integer.MIN_VALUE, 0, 0, 0, 0, minValue + ".00:00:00");
    }

    @Test
    public void testMaxPositiveDays() {
        Duration duration = new Duration(Integer.MAX_VALUE, 0, 0, 0, 0);

        String maxValue = String.valueOf(Integer.MAX_VALUE);
        verify(duration, Integer.MAX_VALUE, 0, 0, 0, 0, maxValue + ".00:00:00");
    }

    @Test
    public void testMinNegativeHours() {
        Duration duration = new Duration(0, -23, 0, 0, 0);

        verify(duration, 0, -23, 0, 0, 0, "-23:00:00");
    }

    @Test
    public void testMaxPositiveHours() {
        Duration duration = new Duration(0, 23, 0, 0, 0);

        verify(duration, 0, 23, 0, 0, 0, "23:00:00");
    }

    @Test
    public void testMinNegativeMinutes() {
        Duration duration = new Duration(0, 0, -59, 0, 0);

        verify(duration, 0, 0, -59, 0, 0, "00:-59:00");
    }

    @Test
    public void testMaxPositiveMinutes() {
        Duration duration = new Duration(0, 0, 59, 0, 0);

        verify(duration, 0, 0, 59, 0, 0, "00:59:00");
    }

    @Test
    public void testMinNegativeSeconds() {
        Duration duration = new Duration(0, 0, 0, -59, 0);

        verify(duration, 0, 0, 0, -59, 0, "00:00:-59");
    }

    @Test
    public void testMaxPositiveSeconds() {
        Duration duration = new Duration(0, 0, 0, 59, 0);

        verify(duration, 0, 0, 0, 59, 0, "00:00:59");
    }

    @Test
    public void testMaxMilliseconds() {
        Duration duration = new Duration(0, 0, 0, 0, 999);

        verify(duration, 0, 0, 0, 0, 999, "00:00:00.9990000");
    }

    @Test
    public void testOneHourTwoMinThreeSec() {
        Duration duration = new Duration(0, 1, 2, 3, 0);

        verify(duration, 0, 1, 2, 3, 0, "01:02:03");
    }

    @Test
    public void test2Days0Hours2Min3Sec() {
        Duration duration = new Duration(2, 0, 2, 3, 0);

        verify(duration, 2, 0, 2, 3, 0, "02.00:02:03");
    }

    @Test
    public void test0Days0Hours2Min3Sec() {
        Duration duration = new Duration(0, 0, 2, 3, 0);

        verify(duration, 0, 0, 2, 3, 0, "00:02:03");
    }

    @Test
    public void test250Milli() {
        Duration duration = new Duration(0, 0, 0, 0, 250);

        verify(duration, 0, 0, 0, 0, 250, "00:00:00.2500000");
    }

    @Test
    public void testMilliCtorWithLongMaxValue() {
        Duration duration = new Duration(Long.MAX_VALUE);

        verify(duration, 106751991167L, 7, 12, 55, 807, "106751991167.07:12:55.8070000");
    }

    @Test
    public void test1DayAndOneMilliWithMilliCtor() {
        Duration duration = new Duration(86400001);

        verify(duration, 1, 0, 0, 0, 1, "01.00:00:00.0010000");
    }

    @Test
    public void test250MilliWithMilliCtor() {
        Duration duration = new Duration(250);

        verify(duration, 0, 0, 0, 0, 250, "00:00:00.2500000");
    }

    @Test
    public void test99Days23Hours59Min59Sec999Milli() {
        Duration duration = new Duration(99, 23, 59, 59, 999);

        verify(duration, 99, 23, 59, 59, 999, "99.23:59:59.9990000");
    }

    @Test
    public void test3Hours() {
        Duration duration = new Duration(0, 3, 0, 0, 0);

        verify(duration, 0, 3, 0, 0, 0, "03:00:00");
    }

    @Test
    public void testTotalMilliseconds() {
        Duration duration = new Duration(1, 1, 1, 1, 1);

        // 90061001 ms is 1 day, 1 hour, 1 minute, 1 sec and 1 milliseconds.
        Assert.assertEquals(90061001, duration.getTotalMilliseconds());
    }

    @Test
    public void test25Milli() {
        Duration duration = new Duration(0, 0, 0, 0, 25);

        verify(duration, 0, 0, 0, 0, 25, "00:00:00.0250000");
    }

    @Test
    public void test25MilliWithMilliCtor() {
        Duration duration = new Duration(25);

        verify(duration, 0, 0, 0, 0, 25, "00:00:00.0250000");
    }

    @Test
    public void testEquals() {
        Duration duration1 = new Duration(25);
        Duration duration2 = new Duration(0, 0, 0, 0, 25);

        assertEquals(duration1, duration2);
    }

    private static void verify(Duration duration,
                               long expectedDays,
                               int expectedHours,
                               int expectedMinutes,
                               int expectedSeconds,
                               int expectedMilliseconds,
                               String expectedString) {
        assertEquals(expectedString, duration.toString());
        assertEquals(expectedDays, duration.getDays());
        assertEquals(expectedHours, duration.getHours());
        assertEquals(expectedMinutes, duration.getMinutes());
        assertEquals(expectedSeconds, duration.getSeconds());
        assertEquals(expectedMilliseconds, duration.getMilliseconds());
    }

}
