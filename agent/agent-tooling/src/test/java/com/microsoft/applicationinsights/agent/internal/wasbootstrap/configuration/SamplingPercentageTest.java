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

package com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration;

import org.junit.*;

import static com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.ConfigurationBuilder.roundToNearest;
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
}
