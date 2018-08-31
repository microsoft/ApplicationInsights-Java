package com.microsoft.applicationinsights.channel.concrete.localforwarder;


import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter.TelemetriesFetcher;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.localforwarder.library.inputs.contracts.AITelemetryServiceGrpc.AITelemetryServiceImplBase;
import com.microsoft.localforwarder.library.inputs.contracts.AiResponse;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import com.microsoft.localforwarder.library.inputs.contracts.TelemetryBatch;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalForwarderTelemetryTransmitterTest {

    private LocalForwarderTelemetriesTransmitter underTest;
    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private AITelemetryServiceImplBase fakeServer;

    @Before
    public void setup() throws IOException {
        StopWatch sw = StopWatch.createStarted();
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .fallbackHandlerRegistry(serviceRegistry)
                        .directExecutor()
                        .build()
                        .start());

        fakeServer = new AITelemetryServiceImplBase() {
            @Override
            public StreamObserver<TelemetryBatch> sendTelemetryBatch(final StreamObserver<AiResponse> responseObserver) {
                System.out.println("Server: Got a batch!");
                return new StreamObserver<TelemetryBatch>() {
                    @Override
                    public void onNext(TelemetryBatch value) {
                        System.out.println("Server NEXT: " + value);
                        responseObserver.onNext(AiResponse.newBuilder().build());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("Server ERROR: " + t.toString());
                        responseObserver.onError(t);
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Server DONE!");
                        responseObserver.onCompleted();
                    }
                };
            }
        };
        serviceRegistry.addService(fakeServer);

        underTest = new LocalForwarderTelemetriesTransmitter(InProcessChannelBuilder.forName(serverName).directExecutor(), false, 0);
        System.out.printf("%s.setup() took %.3f seconds%n", LocalForwarderTelemetryTransmitterTest.class.getSimpleName(), sw.getTime(TimeUnit.MILLISECONDS) / 1000.0);
    }

    @After
    public void tearDown() {
        StopWatch sw = StopWatch.createStarted();
        underTest.stop(10, TimeUnit.SECONDS);
        underTest = null;
        System.out.printf("%s.tearDown() took %.3f seconds%n", LocalForwarderTelemetryTransmitterTest.class.getSimpleName(), sw.getTime(TimeUnit.MILLISECONDS) / 1000.0);
    }

    @Test(expected = NullPointerException.class)
    public void nullChannelBuilderToContructorThrows() {
        underTest = new LocalForwarderTelemetriesTransmitter(null, false, 0);
    }

    private StreamObserver<AiResponse> createDefaultResponseObserverForLatch(final CountDownLatch latch) {
        return new StreamObserver<AiResponse>() {
            @Override
            public void onNext(AiResponse value) {
                System.out.println("sendNow NEXT: "+value);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("sendNow ERROR: "+t.toString());
            }

            @Override
            public void onCompleted() {
                latch.countDown();
                System.out.println("sendNow DONE!");
            }
        };
    }

    @Test
    public void sendNowSendsBatch() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        underTest.setResponseObserver(createDefaultResponseObserverForLatch(latch));

        boolean success = underTest.sendNow(generateTelemetryCollection());
        assertTrue("sendNow should return true", success);
        assertTrue("timeout waiting for response", latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void scheduleSendSendsBatch() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        underTest.setResponseObserver(createDefaultResponseObserverForLatch(latch));

        TelemetriesFetcher<Telemetry> mockFetcher = mock(TelemetriesFetcher.class);
        when(mockFetcher.fetch()).thenReturn(generateTelemetryCollection());
        boolean success = underTest.scheduleSend(mockFetcher, 2, TimeUnit.SECONDS);

        assertTrue("sendNow should return true", success);
        assertTrue("timeout waiting for response", latch.await(5, TimeUnit.SECONDS));
    }

    private Collection<Telemetry> generateTelemetryCollection() {
        Collection<Telemetry> rval = new ArrayList<>();

        RequestTelemetry rt = new RequestTelemetry("test request", new Date(), 1234, "211", true);
        rt.getContext().setInstrumentationKey("fake-ikey");

        rval.add(LocalForwarderModelTransformer.transform(rt));

        return rval;
    }

}
