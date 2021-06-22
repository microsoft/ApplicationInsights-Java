package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.TelemetryChannel;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.TEMPORARY_FILE_EXTENSION;

/**
 * This class manages loading a list of {@link ByteBuffer} from the disk.
 */
public class LocalFileLoader implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileLoader.class);
    private static final long INTERVAL_SECONDS = 30; // send persisted telemetries from local disk every 30 seconds.
    private static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(LocalFileLoader.class));

    private final LocalFileCache localFileCache;
    private final TelemetryChannel telemetryChannel;

    public static void start(LocalFileCache localFileCache, TelemetryChannel telemetryChannel) {
        LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, telemetryChannel);
        scheduledExecutor.scheduleWithFixedDelay(localFileLoader, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    // visible for tests
    LocalFileLoader(LocalFileCache localFileCache, TelemetryChannel telemetryChannel) {
        this.localFileCache = localFileCache;
        this.telemetryChannel = telemetryChannel;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = loadTelemetriesFromDisk();
            if (buffer != null) {
                telemetryChannel.sendRawBytes(buffer);
            }
        } catch (RuntimeException ex) {
            logger.error("Error occurred while sending telemetries from the local storage.", ex);
            // TODO (heya) track sending persisted telemetries failure via Statsbeat.
        }
    }

    // Load ByteBuffer from persisted files on disk in FIFO order.
    ByteBuffer loadTelemetriesFromDisk() {
        String filenameToBeLoaded = localFileCache.poll();
        if (filenameToBeLoaded == null) {
            return null;
        }

        File tempFile = PersistenceHelper.renameFileExtension(filenameToBeLoaded, TEMPORARY_FILE_EXTENSION);
        if (tempFile == null) {
            return null;
        }

        return read(tempFile);
    }

    private static ByteBuffer read(File file) {
        try {
            // TODO (trask) optimize this by reading directly into ByteBuffer(s)
            byte[] result = Files.readAllBytes(file.toPath());

            // TODO (heya) backoff and retry delete when it fails.
            Files.delete(file.toPath());

            return ByteBuffer.wrap(result);
        } catch (IOException ex) {
            // TODO (heya) track deserialization failure via Statsbeat
            logger.error("Fail to deserialize objects from  {}", file.getName(), ex);
            return null;
        } catch(SecurityException ex) {
            logger.error("Unable to delete {}. Access is denied.", file.getName(), ex);
            return null;
        }
    }
}
