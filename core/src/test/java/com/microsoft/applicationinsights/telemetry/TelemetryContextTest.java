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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public final class TelemetryContextTest {
    private final static String TEST_IKEY = "00000000-0000-0000-0000-000000000000";

    @Test
    public void testCtor() {
        TelemetryContext context = new TelemetryContext();

        assertTrue(context.getProperties().isEmpty());
        assertTrue(context.getTags().isEmpty());
        assertNull(context.getInstrumentationKey());
    }

    @Test
    public void testSetInstrumentationKey() {
        TelemetryContext context = new TelemetryContext();
        context.setInstrumentationKey("key");

        assertEquals("key", context.getInstrumentationKey());
    }

    @Test
    public void testEmptyInstrumentationKeyOverridenWhenContextInitialized() {
        TelemetryContext contextToInitialize = new TelemetryContext();

        TelemetryContext context = new TelemetryContext();
        context.setInstrumentationKey(TEST_IKEY);

        contextToInitialize.initialize(context);

        Assert.assertEquals(TEST_IKEY, contextToInitialize.getInstrumentationKey());
    }

    @Test
    public void testInstrumentationKeyNotOverridenWhenContextInitialized() {
        TelemetryContext contextToInitialize = new TelemetryContext();
        contextToInitialize.setInstrumentationKey(TEST_IKEY);

        TelemetryContext context = new TelemetryContext();
        context.setInstrumentationKey(TEST_IKEY.replaceFirst("0", "1"));

        contextToInitialize.initialize(context);

        Assert.assertEquals(TEST_IKEY, contextToInitialize.getInstrumentationKey());
    }
}
