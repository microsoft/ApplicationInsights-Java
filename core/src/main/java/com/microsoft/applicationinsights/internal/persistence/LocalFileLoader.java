package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.TelemetryChannel;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
    private static final long INTERVAL = TimeUnit.SECONDS.toSeconds(30); // send persisted telemetries from local disk every 30 seconds.
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(LocalFileLoader.class));
    private static TelemetryChannel telemetryChannel;
    private static final LocalFileLoader INSTANCE = new LocalFileLoader();

    /**
     * Track a list of active filenames persisted on disk.
     * FIFO (First-In-First-Out) read will avoid an additional sorting at every read.
     */
    private static final Queue<String> PERSISTED_FILES_QUEUE = new ConcurrentLinkedDeque<>();

    public static LocalFileLoader get() {
        return INSTANCE;
    }

    // Track the newly persisted filename to the concurrent hashmap.
    void addPersistedFilenameToMap(String filename) {
        PERSISTED_FILES_QUEUE.add(filename);
    }

    // Load List<ByteBuffer> from persisted files on disk in FIFO order.
    byte[] loadTelemetriesFromDisk() {
        String filenameToBeLoaded = PERSISTED_FILES_QUEUE.poll();
        if (filenameToBeLoaded == null) {
            logger.warn("PERSISTED_FILES_QUEUE is empty.");
            return null;
        }

        File tempFile = PersistenceHelper.renameFileExtension(filenameToBeLoaded, TEMPORARY_FILE_EXTENSION);
        if (tempFile == null) {
            return null;
        }


        return read(tempFile);
    }

    // Used by tests only
    Queue<String> getPersistedFilesQueue() {
        return PERSISTED_FILES_QUEUE;
    }

    private byte[] read(File file) {
        byte[] result = null;
        try {
            result = Files.readAllBytes(file.toPath());

            // TODO (heya) backoff and retry delete when it fails. 
            Files.delete(file.toPath());
        } catch (IOException ex) {
            // TODO (heya) track deserialization failure via Statsbeat
            logger.error("Fail to deserialize objects from  {}", file.getName(), ex);
        } catch(SecurityException ex) {
            logger.error("Unable to delete {}. Access is denied.", file.getName(), ex);
        }

        return result;
    }

    private LocalFileLoader() {
        scheduledExecutor.scheduleWithFixedDelay(new PersistedTelemetriesSender(), INTERVAL, INTERVAL, TimeUnit.SECONDS);
        try {
            telemetryChannel = TelemetryChannel.create(new EndpointProvider().getIngestionEndpoint().toURL());
        } catch (MalformedURLException ex) {
            logger.error("Fail to create TelemetryChannel.", ex);
        }
    }

    private class PersistedTelemetriesSender implements Runnable {
        @Override
        public void run() {
            try {
                byte[] rawBytes = loadTelemetriesFromDisk();
                if (rawBytes != null) {
                    telemetryChannel.sendRawBytes(rawBytes);
                }
            } catch (Exception ex) {
                logger.error("Error occurred while sending telemetries from the local storage.", ex);
                // TODO (heya) track sending persisted telemetries failure via Statsbeat.
            }
        }
    }
}
