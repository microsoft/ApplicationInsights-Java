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

package com.microsoft.applicationinsights.agent.internal.agent;

import org.junit.Test;

import static org.junit.Assert.*;

public final class MethodInstrumentationRequestTest {
    private final static String MOCK_METHOD_NAME = "method-name";
    private final static String MOCK_METHOD_SIGNATURE = "method-signature";

    @Test(expected = IllegalArgumentException.class)
    public void testCtor1EmptyMethodName() {
        new MethodInstrumentationRequest("", MOCK_METHOD_SIGNATURE, false, true, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtor1NullMethodName() {
        new MethodInstrumentationRequest(null, MOCK_METHOD_SIGNATURE, false, true, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtor2EmptyMethodName() {
        new MethodInstrumentationRequest("", false, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtor2NullMethodName() {
        new MethodInstrumentationRequest(null, false, true);
    }

    @Test
    public void testCtor1() {
        MethodInstrumentationRequest test = new MethodInstrumentationRequest(MOCK_METHOD_NAME, MOCK_METHOD_SIGNATURE, false, true, 0);

        assertEquals(MOCK_METHOD_NAME, test.getMethodName());
        assertEquals(MOCK_METHOD_SIGNATURE, test.getMethodSignature());
        assertFalse(test.isReportCaughtExceptions());
        assertTrue(test.isReportExecutionTime());
    }

    @Test
    public void testCtor1WithNullSignature() {
        MethodInstrumentationRequest test = new MethodInstrumentationRequest(MOCK_METHOD_NAME, null, false, true, 0);

        assertEquals(MOCK_METHOD_NAME, test.getMethodName());
        assertNull(test.getMethodSignature());
        assertFalse(test.isReportCaughtExceptions());
        assertTrue(test.isReportExecutionTime());
    }

    @Test
    public void testCtor2() {
        MethodInstrumentationRequest test = new MethodInstrumentationRequest(MOCK_METHOD_NAME, false, true);

        assertEquals(MOCK_METHOD_NAME, test.getMethodName());
        assertNull(test.getMethodSignature());
        assertFalse(test.isReportCaughtExceptions());
        assertTrue(test.isReportExecutionTime());
    }
}


