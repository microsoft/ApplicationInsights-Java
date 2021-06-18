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

    public boolean writeToDisk(Object object) {
        return internalWrite(object);
    }

    private boolean internalWrite(Object object) {
        if (!PersistenceHelper.maxFileSizeExceeded()) {
            return false;
        }

        File tempFile = PersistenceHelper.createTempFileWithUniqueName();
        if (tempFile == null) {
            return false;
        }

        if (!write(tempFile, object)) {
            return false;
        }

        File permanentFile = PersistenceHelper.renameFileExtension(tempFile.getName(), PERMANENT_FILE_EXTENSION);
        if (permanentFile == null) {
            return false;
        }

        LocalFileLoader.get().addPersistedFilenameToMap(permanentFile.getName());

        logger.info("List<ByteBuffers> has been persisted to file and will be sent when the network becomes available.");
        // TODO (heya) track data persistence success via Statsbeat
        return true;
    }

    private boolean write(File file, Object object) {
        List<ByteBuffer> byteBuffers = null;
        byte[] rawBytes = null;
        if (object instanceof List<?>) {
            byteBuffers = (List<ByteBuffer>) object;
        } else if (object instanceof byte[]) {
            rawBytes = (byte[]) object;
        }
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            if (byteBuffers != null) {
                for (ByteBuffer byteBuffer : byteBuffers) {
                    byteBuffer.position(0);
                    out.write(byteBuffer.array());
                }
            } else if (rawBytes != null) {
                out.write(rawBytes);
            }
        } catch (IOException ex) {
            // TODO (heya) track IO write failure via Statsbeat
            logger.error("Fail to write to file.", ex);
            return false;
        }

        return true;
    }
}
