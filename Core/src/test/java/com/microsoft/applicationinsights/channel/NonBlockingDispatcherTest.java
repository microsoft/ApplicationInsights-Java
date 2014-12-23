package com.microsoft.applicationinsights.channel;

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