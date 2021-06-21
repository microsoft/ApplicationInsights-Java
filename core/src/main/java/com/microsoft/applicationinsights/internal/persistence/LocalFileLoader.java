package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.TelemetryChannel;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.DEFAULT_FOLDER;
import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.PERMANENT_FILE_EXTENSION;
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
     * Caveat: data loss happens when the app crashes.  filenames stored in this queue will be lost forever.
     * There isn't an unique way to identify each java app.  C# uses "User@processName" to identify each app, but
     * Java can't rely on process name since it's a system property that can be customized via the command line.
     * TODO (heya) need to uniquely identify each app and figure out how to retrieve data from the disk for each app.
     */
    private static final Queue<String> persistedFilesCache = new ConcurrentLinkedDeque<>();

    public static LocalFileLoader get() {
        return INSTANCE;
    }

    // Track the newly persisted filename to the concurrent hashmap.
    void addPersistedFilenameToMap(String filename) {
        persistedFilesCache.add(filename);
    }

    // Load List<ByteBuffer> from persisted files on disk in FIFO order.
    byte[] loadTelemetriesFromDisk() {
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

    private String loadOldestFromCache() {
        if (persistedFilesCache.isEmpty()) { // if the cache is empty because of app crashes, reload everything from disk
            Collection<File> filesFromDisk = FileUtils.listFiles(DEFAULT_FOLDER, new String[]{PERMANENT_FILE_EXTENSION}, false);
            if (filesFromDisk.isEmpty()) {
                return null;
            }

            List<File> files = sortPersistedFiles(filesFromDisk);
            if (files == null || files.isEmpty()) {
                return null;
            }

            persistedFilesCache.addAll(files.stream().map(File::getName).collect(Collectors.toList()));
        }

        String fileToBeLoaded = persistedFilesCache.poll();

        return fileToBeLoaded != null ? fileToBeLoaded : null;

    }

    List<File> sortPersistedFiles(Collection<File> files) {
        List<File> result = new ArrayList<>(files);
        Collections.sort(result, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return getMillisecondsFromFilename(o1).compareTo(getMillisecondsFromFilename(o2));
            }
        });

        return result;
    }

    private Long getMillisecondsFromFilename(File file) {
        String filename = file.getName();
        String milliSeconds = filename.substring(0, filename.lastIndexOf('-'));
        try {
            return Long.parseLong(milliSeconds);
        } catch (NumberFormatException ex) {
            logger.error("Fail to convert milliseconds in string to long", ex);
        }

        return null;
    }

    // Used by tests only
    Queue<String> getPersistedFilesCache() {
        return persistedFilesCache;
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
        telemetryChannel = TelemetryChannel.create(new EndpointProvider().getIngestionEndpoint());
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
