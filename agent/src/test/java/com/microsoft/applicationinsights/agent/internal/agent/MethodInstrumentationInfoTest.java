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

public final class MethodInstrumentationInfoTest {
    private final static String MOCK_METHOD_NAME_1 = "mock_method_1";
    private final static String MOCK_METHOD_SIGNATURE_1 = "mock_signature_1";
    private final static String MOCK_METHOD_SIGNATURE_2 = "mock_signature_2";

    private final static String MOCK_METHOD_NAME_2 = "mock_method_2";

    @Test
    public void testEmpty() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        assertTrue(info.isEmpty());
    }

    @Test
    public void testAddOneMethodWithSignature() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, false, true));

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
    }

    @Test
    public void testAddTwoMethodsWithSignature() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));
        info.addMethod(createRequest(MOCK_METHOD_NAME_2, MOCK_METHOD_SIGNATURE_2, true, false));

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
        verifyDecision(info, MOCK_METHOD_NAME_2, MOCK_METHOD_SIGNATURE_2, true, false);
    }

    @Test
    public void testAddOneMethodsWithTwoSignatures() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_2, true, false));

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_2, true, false);

        MethodInstrumentationDecision decision = info.getDecision(MOCK_METHOD_NAME_2, MOCK_METHOD_SIGNATURE_1);
        assertNull(decision);

        decision = info.getDecision(MOCK_METHOD_NAME_2, MOCK_METHOD_SIGNATURE_2);
        assertNull(decision);
    }

    @Test
    public void testAddOneMethodsWithAllSignaturesEnabled() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, false, true));

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_2, false, true);
    }

    @Test
    public void testAddMethodsWithDifferentKindOfSignatures() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();

        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, true, true));

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_2, true, true);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddAllMethodsWhileThereAreMethods() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));

        info.addAllMethods(true, false);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddMethodAfterAllMethodsCalled() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addAllMethods(true, false);

        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));
    }

    @Test
    public void testAddAllMethods() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addAllMethods(true, false);

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, true, false);
        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_2, true, false);
        verifyDecision(info, MOCK_METHOD_NAME_2, MOCK_METHOD_SIGNATURE_2, true, false);
    }

    @Test
    public void testAddMethodWithAllFalseReportsAttribute() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addAllMethods(false, false);

        assertTrue(info.isEmpty());

        MethodInstrumentationDecision decision = info.getDecision(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1);
        assertNull(decision);
    }

    @Test
    public void testAddMethodsOneWithAllFalse() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));
        info.addMethod(createRequest(MOCK_METHOD_NAME_2, false, false));

        assertFalse(info.isEmpty());

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);

        MethodInstrumentationDecision decision = info.getDecision(MOCK_METHOD_NAME_2, MOCK_METHOD_SIGNATURE_2);
        assertNull(decision);
    }

    @Test
    public void testAddMethodsTwiceWithDifferentAttributes() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, false));

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
    }

    @Test
    public void testAddMethodsTwiceWithDifferentAttributesFirstWithAllFalse() {
        MethodInstrumentationInfo info = new MethodInstrumentationInfo();
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, false));
        info.addMethod(createRequest(MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true));

        verifyDecision(info, MOCK_METHOD_NAME_1, MOCK_METHOD_SIGNATURE_1, false, true);
    }

    private static void verifyDecision(MethodInstrumentationInfo info,
                                       String methodName,
                                       String methodSignature,
                                       boolean expectedReportCaughtExceptions,
                                       boolean expectedReportExecutionTime) {
        MethodInstrumentationDecision decision = info.getDecision(methodName, methodSignature);
        assertNotNull(decision);
        assertEquals(decision.reportExecutionTime, expectedReportExecutionTime);
        assertEquals(decision.reportCaughtExceptions, expectedReportCaughtExceptions);
    }

    private static MethodInstrumentationRequest createRequest(String methodName, boolean reportCaughtExceptions, boolean reportExecutionTime) {
        return createRequest(methodName, null, reportCaughtExceptions, reportExecutionTime);
    }

    private static MethodInstrumentationRequest createRequest(String methodName, String signature, boolean reportCaughtExceptions, boolean reportExecutionTime) {
        MethodInstrumentationRequest request =
                new MethodInstrumentationRequestBuilder()
                        .withMethodName(methodName)
                        .withMethodSignature(signature)
                        .withReportCaughtExceptions(reportCaughtExceptions)
                        .withReportExecutionTime(reportExecutionTime).create();

        return request;
    }
}
