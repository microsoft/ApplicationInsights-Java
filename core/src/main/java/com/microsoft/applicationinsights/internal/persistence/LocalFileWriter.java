package com.microsoft.applicationinsights.internal.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.DEFAULT_FOlDER;
import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.PERMANENT_FILE_EXTENSION;

/**
 * This class manages writing a list of {@link ByteBuffer} to the file system.
 */
public final class LocalFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileWriter.class);

    public LocalFileWriter() {
        if (!DEFAULT_FOlDER.exists()) {
            DEFAULT_FOlDER.mkdir();
        }

        if (!DEFAULT_FOlDER.exists() || !DEFAULT_FOlDER.canRead() || !DEFAULT_FOlDER.canWrite()) {
            throw new IllegalArgumentException("{} must exist with read and write permissions.");
        }
    }

    public boolean writeToDisk(List<ByteBuffer> byteBuffers) {
        if (!PersistenceHelper.maxFileSizeExceeded()) {
            return false;
        }

        File file = PersistenceHelper.createTempFileWithUniqueName();
        if (file == null) {
            return false;
        }

        if (!saveByteBuffers(file, byteBuffers)) {
            return false;
        }

        if (PersistenceHelper.renameFileExtension(file.getName(), PERMANENT_FILE_EXTENSION) == null) {
            return false;
        }

        LocalFileLoader.get().addPersistedFilenameToMap(file.getName());

        logger.info("List<ByteBuffers> has been persisted to file and will be sent when the network becomes available.");
        // TODO (heya) track data persistence success via Statsbeat
        return true;
    }

    private boolean saveByteBuffers(File file, List<ByteBuffer> byteBuffers) {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            for (ByteBuffer byteBuffer : byteBuffers) {
                byteBuffer.position(0);
                out.write(byteBuffer.array());
            }
        } catch (IOException ex) {
            // TODO (heya) track IO write failure via Statsbeat
            logger.error("Fail to write to file.", ex);
            return false;
        }

        return true;
    }
}
