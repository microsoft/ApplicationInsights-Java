package com.microsoft.applicationinsights.internal.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.TEMPORARY_FILE_EXTENSION;

/**
 * This class manages loading a list of {@link ByteBuffer} from the disk.
 */
public class LocalFileLoader {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileLoader.class);

    private final LocalFileCache localFileCache;

    public LocalFileLoader(LocalFileCache localFileCache) {
        this.localFileCache = localFileCache;
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
            // TODO (trask) optimization: read this directly into ByteBuffer(s)
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
