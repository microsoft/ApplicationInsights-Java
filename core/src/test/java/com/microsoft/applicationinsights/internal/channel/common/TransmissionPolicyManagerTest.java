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

package com.microsoft.applicationinsights.internal.channel.common;

import org.junit.Test;

import static org.junit.Assert.*;

public final class TransmissionPolicyManagerTest {
    @Test
    public void testAfterCtor() {
        TransmissionPolicyManager tested = new TransmissionPolicyManager();
        assertNotNull(tested.getTransmissionPolicyState());
        assertEquals(tested.getTransmissionPolicyState().getCurrentState(), TransmissionPolicy.UNBLOCKED);
    }

    @Test
    public void testSuspendInSecondsWithUnBlock() {
        TransmissionPolicyManager tested = new TransmissionPolicyManager();
        tested.suspendInSeconds(TransmissionPolicy.UNBLOCKED, 10);
        assertEquals(tested.getTransmissionPolicyState().getCurrentState(), TransmissionPolicy.UNBLOCKED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSuspendInSecondsWithZeroSeconds() {
        TransmissionPolicyManager tested = new TransmissionPolicyManager();
        tested.suspendInSeconds(TransmissionPolicy.BLOCKED_AND_CANNOT_BE_PERSISTED, 0);
    }

    @Test
    public void testSuspendInSecondsWithTwoWhereTheFirstOneCounts() throws InterruptedException {
        TransmissionPolicyManager tested = new TransmissionPolicyManager();
        tested.suspendInSeconds(TransmissionPolicy.BLOCKED_AND_CANNOT_BE_PERSISTED, 2);
        tested.suspendInSeconds(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, 1);

        Thread.sleep(1500);
        assertEquals(tested.getTransmissionPolicyState().getCurrentState(), TransmissionPolicy.BLOCKED_AND_CANNOT_BE_PERSISTED);
        Thread.sleep(1000);
        assertEquals(tested.getTransmissionPolicyState().getCurrentState(), TransmissionPolicy.UNBLOCKED);
    }

    @Test
    public void testSuspendInSecondsWithTwoWhereTheSecondOneCounts() throws InterruptedException {
        TransmissionPolicyManager tested = new TransmissionPolicyManager();
        tested.suspendInSeconds(TransmissionPolicy.BLOCKED_AND_CANNOT_BE_PERSISTED, 1);
        tested.suspendInSeconds(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, 2);

        Thread.sleep(1500);
        assertEquals(tested.getTransmissionPolicyState().getCurrentState(), TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED);
        Thread.sleep(1000);
        assertEquals(tested.getTransmissionPolicyState().getCurrentState(), TransmissionPolicy.UNBLOCKED);
    }
}
