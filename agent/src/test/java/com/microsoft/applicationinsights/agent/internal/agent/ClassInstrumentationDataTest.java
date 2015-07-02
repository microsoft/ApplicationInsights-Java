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

import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ClassInstrumentationDataTest {
    private final static String MOCK_CLASS_NAME = "ClassName";
    private final static String MOCK_METHOD = "Method";
    private final static String MOCK_SIGNATURE_1 = "Signature1";
    private final static String MOCK_SIGNATURE_2 = "Signature2";
    private final static String MOCK_SIGNATURE_3 = "Signature3";

    @Test
    public void testCtor() throws Exception {
        ClassInstrumentationData test = new ClassInstrumentationData(MOCK_CLASS_NAME, InstrumentedClassType.HTTP)
                .setReportCaughtExceptions(true)
                .setReportExecutionTime(false);
        assertTrue(test.getMethodInstrumentationInfo().isEmpty());
        assertEquals(test.getClassType(), InstrumentedClassType.HTTP);
        assertEquals(test.getClassName(), MOCK_CLASS_NAME);
        assertEquals(test.isReportCaughtExceptions(), true);
        assertEquals(test.isReportExecutionTime(), false);
    }

    @Test
    public void testAddMethodOverridesBooleanValues() throws Exception {
        ClassInstrumentationData test = new ClassInstrumentationData(MOCK_CLASS_NAME, InstrumentedClassType.HTTP)
                .setReportCaughtExceptions(false)
                .setReportExecutionTime(false);
        test.addMethod(MOCK_METHOD, MOCK_SIGNATURE_1, true, true);

        MethodInstrumentationDecision decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_1);

        assertNotNull(decision);
        assertEquals(decision.isReportCaughtExceptions(), true);
        assertEquals(decision.isReportExecutionTime(), true);

        decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_2);

        assertNull(decision);
    }

    @Test
    public void testAddMethodsWithDistinctSignatures() throws Exception {
        ClassInstrumentationData test = new ClassInstrumentationData(MOCK_CLASS_NAME, InstrumentedClassType.HTTP)
                .setReportCaughtExceptions(false)
                .setReportExecutionTime(false);
        test.addMethod(MOCK_METHOD, MOCK_SIGNATURE_1, true, true);
        test.addMethod(MOCK_METHOD, MOCK_SIGNATURE_2, false, true);

        MethodInstrumentationDecision decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_1);

        assertNotNull(decision);
        assertEquals(decision.isReportCaughtExceptions(), true);
        assertEquals(decision.isReportExecutionTime(), true);

        decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_2);

        assertNotNull(decision);
        assertEquals(decision.isReportCaughtExceptions(), false);
        assertEquals(decision.isReportExecutionTime(), true);
    }

    @Test
    public void testAddMethodsWithDistinctSignaturesAndOneWithNoSignature() throws Exception {
        ClassInstrumentationData test = new ClassInstrumentationData(MOCK_CLASS_NAME, InstrumentedClassType.HTTP)
                .setReportCaughtExceptions(true)
                .setReportExecutionTime(false);
        test.addMethod(MOCK_METHOD, MOCK_SIGNATURE_1, true, true);
        test.addMethod(MOCK_METHOD, MOCK_SIGNATURE_2, false, true);
        test.addMethod(MOCK_METHOD, null, false, true);

        MethodInstrumentationDecision decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_1);

        assertNotNull(decision);
        assertEquals(decision.isReportCaughtExceptions(), true);
        assertEquals(decision.isReportExecutionTime(), true);

        decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_2);

        assertNotNull(decision);
        assertEquals(decision.isReportCaughtExceptions(), false);
        assertEquals(decision.isReportExecutionTime(), true);

        decision = test.getDecisionForMethod(MOCK_METHOD, MOCK_SIGNATURE_3);

        assertNotNull(decision);
        assertEquals(decision.isReportCaughtExceptions(), false);
        assertEquals(decision.isReportExecutionTime(), true);
    }
}