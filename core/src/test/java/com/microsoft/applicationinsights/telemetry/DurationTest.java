/*
 * AppInsights-Java
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DurationTest {
    @Test
    public void testZero() {
        Duration duration = new Duration(0);

        verify(duration, 0, 0, 0, 0, 0, "00:00:00");
    }

    @Test
    public void testMinusFourteenDays() {
        Duration duration = new Duration(-14, 0, 0, 0, 0);

        verify(duration, -14, 0, 0, 0, 0, "-14.00:00:00");
    }

    @Test
    public void testOneHourTwoMinThreeSec() {
        Duration duration = new Duration(1, 2, 3);

        verify(duration, 0, 1, 2, 3, 0, "01:02:03");
    }

    @Test
    public void test2Days0Hours2Min3Sec() {
        Duration duration = new Duration(2, 0, 2, 3);

        verify(duration, 2, 0, 2, 3, 0, "02.00:02:03");
    }

    @Test
    public void test0Days0Hours2Min3Sec() {
        Duration duration = new Duration(0, 0, 2, 3);

        verify(duration, 0, 0, 2, 3, 0, "00:02:03");
    }

    @Test
    public void test250Milli() {
        Duration duration = new Duration(0, 0, 0, 0, 250);

        verify(duration, 0, 0, 0, 0, 250, "00:00:00.2500000");
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
        Duration duration = new Duration(3, 0, 0);

        verify(duration, 0, 3, 0, 0, 0, "03:00:00");
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
                               int expectedDays,
                               int expectedHours,
                               int expectedMinutes,
                               int expectedSeconds,
                               int expectedMilliseconds,
                               String expectedString) {
        assertEquals(duration.toString(), expectedString);
        assertEquals(duration.getDays(), expectedDays);
        assertEquals(duration.getHours(), expectedHours);
        assertEquals(duration.getMinutes(), expectedMinutes);
        assertEquals(duration.getSeconds(), expectedSeconds);
        assertEquals(duration.getMilliseconds(), expectedMilliseconds);
    }

}
