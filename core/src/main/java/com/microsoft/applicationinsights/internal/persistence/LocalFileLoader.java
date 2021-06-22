package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.TelemetryChannel;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.TEMPORARY_FILE_EXTENSION;

/**
 * This class manages loading a list of {@link ByteBuffer} from the disk.
 */
public class LocalFileLoader {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileLoader.class);
    private static final long INTERVAL_SECONDS = 30; // send persisted telemetries from local disk every 30 seconds.
    private static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(LocalFileLoader.class));
    private static LocalFileLoader instance;

    private final TelemetryChannel telemetryChannel;

    /**
     * Track a list of active filenames persisted on disk.
     * FIFO (First-In-First-Out) read will avoid an additional sorting at every read.
     * Caveat: data loss happens when the app crashes.  filenames stored in this queue will be lost forever.
     * There isn't an unique way to identify each java app.  C# uses "User@processName" to identify each app, but
     * Java can't rely on process name since it's a system property that can be customized via the command line.
     * TODO (heya) need to uniquely identify each app and figure out how to retrieve data from the disk for each app.
     */
    private static final Queue<String> persistedFilesCache = new ConcurrentLinkedDeque<>();

    private static final Object lock = new Object();

    public static void init(TelemetryChannel telemetryChannel) {
        synchronized (lock) {
            if (instance != null) {
                throw new IllegalArgumentException("init() already called.");
            }
            
            instance = new LocalFileLoader(telemetryChannel);
        }
    }

    public static LocalFileLoader get() {
        if (instance == null) {
            throw new IllegalArgumentException("instance should not be null");
        }

        return instance;
    }

    // Track the newly persisted filename to the concurrent hashmap.
    void addPersistedFilenameToMap(String filename) {
        persistedFilesCache.add(filename);
    }

    // Load ByteBuffer from persisted files on disk in FIFO order.
    ByteBuffer loadTelemetriesFromDisk() {
        String filenameToBeLoaded = persistedFilesCache.poll();
        if (filenameToBeLoaded == null) {
            return null;
        }

        File tempFile = PersistenceHelper.renameFileExtension(filenameToBeLoaded, TEMPORARY_FILE_EXTENSION);
        if (tempFile == null) {
            return null;
        }

        return read(tempFile);
    }

    // Used by tests only
    Queue<String> getPersistedFilesCache() {
        return persistedFilesCache;
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

    private LocalFileLoader(TelemetryChannel telemetryChannel) {
        scheduledExecutor.scheduleWithFixedDelay(new PersistedTelemetriesSender(), INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
        this.telemetryChannel = telemetryChannel;
    }

    private class PersistedTelemetriesSender implements Runnable {
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
    }
}
