package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.TelemetryChannel;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalFileSender implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileSender.class);

    private static final long INTERVAL_SECONDS = 30; // send persisted telemetries from local disk every 30 seconds.
    private static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(LocalFileLoader.class));

    private final LocalFileLoader localFileLoader;
    private final TelemetryChannel telemetryChannel;

    public static void start(LocalFileLoader localFileLoader, TelemetryChannel telemetryChannel) {
        LocalFileSender localFileSender = new LocalFileSender(localFileLoader, telemetryChannel);
        scheduledExecutor.scheduleWithFixedDelay(localFileSender, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private LocalFileSender(LocalFileLoader localFileLoader, TelemetryChannel telemetryChannel) {
        this.localFileLoader = localFileLoader;
        this.telemetryChannel = telemetryChannel;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = localFileLoader.loadTelemetriesFromDisk();
            if (buffer != null) {
                telemetryChannel.sendRawBytes(buffer);
            }
        } catch (RuntimeException ex) {
            logger.error("Error occurred while sending telemetries from the local storage.", ex);
            // TODO (heya) track sending persisted telemetries failure via Statsbeat.
        }
    }
}
