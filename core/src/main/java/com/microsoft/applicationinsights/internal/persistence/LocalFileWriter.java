package com.microsoft.applicationinsights.internal.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.DEFAULT_FOLDER;
import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.PERMANENT_FILE_EXTENSION;

/**
 * This class manages writing a list of {@link ByteBuffer} to the file system.
 */
public final class LocalFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileWriter.class);

    private final LocalFileCache localFileCache;

    public LocalFileWriter(LocalFileCache localFileCache) {
        if (!DEFAULT_FOLDER.exists()) {
            DEFAULT_FOLDER.mkdir();
        }

        if (!DEFAULT_FOLDER.exists() || !DEFAULT_FOLDER.canRead() || !DEFAULT_FOLDER.canWrite()) {
            throw new IllegalArgumentException(DEFAULT_FOLDER + " must exist and have read and write permissions.");
        }

        this.localFileCache = localFileCache;
    }

    public boolean writeToDisk(List<ByteBuffer> buffers) {
        if (!PersistenceHelper.maxFileSizeExceeded()) {
            return false;
        }

        File tempFile = PersistenceHelper.createTempFile();
        if (tempFile == null) {
            return false;
        }

        if (!write(tempFile, buffers)) {
            return false;
        }

        File permanentFile = PersistenceHelper.renameFileExtension(tempFile.getName(), PERMANENT_FILE_EXTENSION);
        if (permanentFile == null) {
            return false;
        }

        localFileCache.addPersistedFilenameToMap(permanentFile.getName());

        logger.info("List<ByteBuffers> has been persisted to file and will be sent when the network becomes available.");
        // TODO (heya) track data persistence success via Statsbeat
        return true;
    }

    private static boolean write(File file, List<ByteBuffer> buffers) {
        try (FileChannel channel = new FileOutputStream(file).getChannel()) {
            for (ByteBuffer byteBuffer : buffers) {
                channel.write(byteBuffer);
            }
            return true;
        } catch (IOException ex) {
            // TODO (heya) track IO write failure via Statsbeat
            logger.error("Fail to write to file.", ex);
            return false;
        }
    }
}
