package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.localforwarder.library.inputs.contracts.AITelemetryServiceGrpc;
import com.microsoft.localforwarder.library.inputs.contracts.AITelemetryServiceGrpc.AITelemetryServiceStub;
import com.microsoft.localforwarder.library.inputs.contracts.AiResponse;
import com.microsoft.localforwarder.library.inputs.contracts.TelemetryBatch;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalForwarderTelemetryTransmitterFactory implements TransmitterFactory<TelemetryBatch> {

    @Override
    public TelemetriesTransmitter<TelemetryBatch> create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        return new LocalForwarderTelemetriesTransmitter(endpoint);
    }

    static final class LocalForwarderTelemetriesTransmitter implements TelemetriesTransmitter<TelemetryBatch> {

        private final String endpoint;
        private final ManagedChannel channel;
        private final AITelemetryServiceStub asyncService;

        // FIXME same as or similar to TransmitterImpl
        private final ScheduledExecutorService executor;
        private static final AtomicInteger INSTANCE_ID_POOL = new AtomicInteger(1);
        private final int instanceId = INSTANCE_ID_POOL.getAndIncrement();

        private final StreamObserver<AiResponse> responseObserver = new StreamObserver<AiResponse>() {
            private final LocalForwarderTelemetriesTransmitter thiz = LocalForwarderTelemetriesTransmitter.this;
            @Override
            public void onNext(AiResponse value) {
                trace("Response received: %s", value.toString());
            }

            @Override
            public void onError(Throwable t) {
                error("Error sending to '%s': %s", thiz.endpoint, ExceptionUtils.getStackTrace(t));
            }

            @Override
            public void onCompleted() {
                trace("Completed");
            }
        };

        public LocalForwarderTelemetriesTransmitter(String endpoint) {
            // FIXME use default when null
            this.endpoint = endpoint;
            this.executor = Executors.newScheduledThreadPool(2,
                    ThreadPoolUtils.createDaemonThreadFactory(LocalForwarderTelemetriesTransmitter.class, instanceId));
            channel = ManagedChannelBuilder.forTarget(endpoint)
                    .enableRetry()
                    .build();
            this.asyncService = AITelemetryServiceGrpc.newStub(channel);
        }

        @Override
        public boolean scheduleSend(final TelemetriesFetcher<TelemetryBatch> telemetriesFetcher, long value, TimeUnit timeUnit) {
            final Collection<TelemetryBatch> batches = telemetriesFetcher.fetch();
            try {
                this.executor.schedule(getSenderRunnable(batches), value, timeUnit);
                return true;
            } catch (Exception e) {
                error("Error in scheduledSend. %d items not sent", batches.size());
            }
            return false;
        }

        private Runnable getSenderRunnable(final Collection<TelemetryBatch> batches) {
            return new Runnable() {
                @Override
                public void run() {
                    StreamObserver<TelemetryBatch> requestObserver = asyncService.sendTelemetryBatch(responseObserver);
                    try {
                        for (TelemetryBatch batch : batches) {
                            trace("Sending request: %s", batch.toString());
                            requestObserver.onNext(batch);
                        }
                        requestObserver.onCompleted();
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t) {
                        requestObserver.onError(t);
                    }
                }
            };
        }

        @Override
        public boolean sendNow(Collection<TelemetryBatch> telemetries) {
            try {
                this.executor.execute(getSenderRunnable(telemetries));
                return true;
            } catch (Exception e) {
                error("Exception from executor: ", ExceptionUtils.getStackTrace(e));
            }
            return false;
        }

        @Override
        public void stop(long timeout, TimeUnit timeUnit) {
            channel.shutdown();
            ThreadPoolUtils.stop(executor, timeout, timeUnit);
        }

        private void error(String format, Object... args) {
            InternalLogger.INSTANCE.error("LocalForwarder: "+format, args);
        }

        private void trace(String format, Object... args) {
            InternalLogger.INSTANCE.trace("LocalForwarder: "+format, args);
        }
    }
}
