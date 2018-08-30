package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter.TelemetriesFetcher;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.localforwarder.library.inputs.contracts.AiResponse;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import org.junit.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class LocalForwarderTelemetryTransmitterTest {

    private ManagedChannel mockChannel = mock(ManagedChannel.class);
    private StreamObserver<AiResponse> mockRespObsvr = mock(StreamObserver.class);

    private LocalForwarderTelemetriesTransmitter underTest;

    @Before
    public void setup() {
        when(mockChannel.shutdown()).thenReturn(mockChannel);
        when(mockChannel.shutdownNow()).thenReturn(mockChannel);

        underTest = new LocalForwarderTelemetriesTransmitter("localhost");
        underTest.updateServiceWithNewChannel(mockChannel);
        underTest.setResponseObserver(mockRespObsvr);
    }

    @After
    public void tearDown() {
        underTest.stop(10, TimeUnit.SECONDS);
        underTest = null;
        reset(mockChannel, mockRespObsvr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullEndpointInInitThrows() {
        underTest = new LocalForwarderTelemetriesTransmitter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyEndpointInInitThrows() {
        underTest = new LocalForwarderTelemetriesTransmitter("");
    }

    @Test
    public void sendNowCreatesCallInChannel() throws InterruptedException {
        boolean success = underTest.sendNow(generateTelemetryCollection());
        assertTrue("sendNow should return true", success);

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(mockRespObsvr).onCompleted();
        assertTrue("timeout waiting for response", latch.await(10, TimeUnit.SECONDS));

        verify(mockChannel, times(1)).newCall(any(MethodDescriptor.class), any(CallOptions.class));
    }

    @Test
    public void scheduleSendCreatesNewCallInChannel() throws InterruptedException {
        TelemetriesFetcher<Telemetry> fetcher = mock(TelemetriesFetcher.class);
        when(fetcher.fetch()).thenReturn(generateTelemetryCollection());

        boolean success = underTest.scheduleSend(fetcher, 2, TimeUnit.SECONDS);
        assertTrue("scheduleSend should return true", success);
        verify(mockChannel, never()).newCall(any(MethodDescriptor.class), any(CallOptions.class));
        TimeUnit.SECONDS.sleep(2);

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(mockRespObsvr).onCompleted();
        assertTrue("timeout waiting for response", latch.await(10, TimeUnit.SECONDS));

        verify(mockChannel, times(1)).newCall(any(MethodDescriptor.class), any(CallOptions.class));
    }

    private Collection<Telemetry> generateTelemetryCollection() {
        Collection<Telemetry> rval = new ArrayList<>();

        RequestTelemetry rt = new RequestTelemetry("test request", new Date(), 1234, "211", true);
        rt.getContext().setInstrumentationKey("fake-ikey");

        rval.add(LocalForwarderModelTransformer.transform(rt));

        return rval;
    }

}
