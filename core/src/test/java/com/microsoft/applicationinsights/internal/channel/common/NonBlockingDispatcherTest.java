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

import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import org.junit.Test;

import org.mockito.Mockito;

import static org.mockito.Matchers.anyObject;

public class NonBlockingDispatcherTest {

    @Test(expected = NullPointerException.class)
    public void nullTest() {
        new NonBlockingDispatcher(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTest() {
        new NonBlockingDispatcher(new TransmissionOutput[]{});
    }

    @Test(expected = NullPointerException.class)
    public void testNullDispatch() throws Exception {
        createDispatcher().dispatch(null);
    }

    @Test
    public void testDispatchSuccessOfFirst() {
        TransmissionOutput mockOutput1 = Mockito.mock(TransmissionOutput.class);
        Mockito.doReturn(true).when(mockOutput1).send((Transmission) anyObject());

        TransmissionOutput mockOutput2 = Mockito.mock(TransmissionOutput.class);

        NonBlockingDispatcher tested = new NonBlockingDispatcher(new TransmissionOutput[] {mockOutput1, mockOutput2});

        Transmission transmission = new Transmission(new byte[2], "mockType", "mockEncoding");
        tested.dispatch(transmission);

        Mockito.verify(mockOutput1, Mockito.times(1)).send((Transmission) anyObject());
        Mockito.verify(mockOutput2, Mockito.never()).send((Transmission) anyObject());
    }

    @Test
    public void testDispatchFailureOfFirst() {
        TransmissionOutput mockOutput1 = Mockito.mock(TransmissionOutput.class);
        Mockito.doReturn(false).when(mockOutput1).send((Transmission) anyObject());

        TransmissionOutput mockOutput2 = Mockito.mock(TransmissionOutput.class);
        Mockito.doReturn(true).when(mockOutput2).send((Transmission) anyObject());

        NonBlockingDispatcher tested = new NonBlockingDispatcher(new TransmissionOutput[] {mockOutput1, mockOutput2});

        Transmission transmission = new Transmission(new byte[2], "mockType", "mockEncoding");
        tested.dispatch(transmission);

        Mockito.verify(mockOutput1, Mockito.times(1)).send((Transmission) anyObject());
        Mockito.verify(mockOutput2, Mockito.times(1)).send((Transmission) anyObject());
    }

    private NonBlockingDispatcher createDispatcher() {
        TransmissionOutput mockOutput1 = Mockito.mock(TransmissionOutput.class);
        TransmissionOutput mockOutput2 = Mockito.mock(TransmissionOutput.class);

        NonBlockingDispatcher tested = new NonBlockingDispatcher(new TransmissionOutput[] {mockOutput1, mockOutput2});
        return tested;
    }
}