package com.microsoft.applicationinsights.internal.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.TEMPORARY_FILE_EXTENSION;

/**
 * This class manages loading a list of {@link ByteBuffer} from the disk.
 */
public class LocalFileLoader {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileLoader.class);
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
    public void addPersistedFilenameToMap(String filename) {
        PERSISTED_FILES_QUEUE.add(filename);
    }

    // Load List<ByteBuffer> from persisted files on disk in FIFO order.
    public byte[] loadFileFromDisk() {
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
            file.delete();
        } catch (IOException ex) {
            // TODO (heya) track deserialization failure via Statsbeat
            logger.error("Fail to deserialize objects from  {}", file.getName(), ex);
        } catch(SecurityException ex) {
            logger.error("Unable to delete {}. Access is denied.", file.getName(), ex);
        }

        return result;
    }

    private LocalFileLoader() {}
}
