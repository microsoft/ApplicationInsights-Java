package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class manages loading a list of {@link ByteBuffer} from the file system.
 */
public class AppInsightsFileLoader {

    private static final Logger logger = LoggerFactory.getLogger(AppInsightsFileLoader.class);
    private static final AppInsightsFileLoader INSTANCE = new AppInsightsFileLoader();

    // Track the actual count of the active files persisted on disk.
    private static final AtomicInteger activeFilesCount = new AtomicInteger();

    // Default folder is located at C:\Users\{USER_NAME}\AppData\Local\Temp\applicationinsights
    static final File DEFAULT_FOlDER = new File(LocalFileSystemUtils.getTempDir(), "applicationinsights");

    /**
     * Track a list of active filenames persisted on disk.
     * FIFO (First-In-First-Out) read will avoid an additional sorting at every read.
     */
    private static final Queue<String> PERSISTED_FILES_QUEUE = new ConcurrentLinkedDeque<>();

    public static AppInsightsFileLoader get() {
        return INSTANCE;
    }

    // Track the newly persisted filename to the concurrent hashmap.
    public void addPersistedFilenameToMap(String filename) {
        PERSISTED_FILES_QUEUE.add(filename);
        logger.debug("# of active persisted files: {}", activeFilesCount.incrementAndGet());
    }

    // Load List<ByteBuffer> from persisted files on disk in FIFO order.
    public byte[] loadFileFromDisk() {
        String filenameToBeLoaded = PERSISTED_FILES_QUEUE.poll();
        if (filenameToBeLoaded == null) {
            logger.warn("PERSISTED_FILES_QUEUE is empty.");
            return null;
        }

        File tempFile = renameToTemporaryName(new File(DEFAULT_FOlDER, filenameToBeLoaded));
        if (!tempFile.exists()) {
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

            // TODO (heya) backoff and retry delete when it fails?
            file.delete();
        } catch (IOException ex) {
            // TODO (heya) track deserialization failure via Statsbeat
            logger.error("Fail to deserialize objects from  {}", file.getName(), ex);
        } catch(SecurityException ex) {
            logger.error("Unable to delete {}. Access is denied.", file.getName(), ex);
        }

        return result;
    }

    private AppInsightsFileLoader() {
    }

    private File renameToTemporaryName(File file) {
        File tempFile = null;
        try {
            tempFile = new File(DEFAULT_FOlDER, FilenameUtils.getBaseName(file.getName()) + ".tmp");
            FileUtils.moveFile(file, tempFile);
        } catch (IOException ex) {
            // TODO (heya) track renaming failure via Statsbeat
            logger.error("Fail to rename file to have the .tmp extension. {}", ex.getCause());
        }

        return tempFile;
    }
}
