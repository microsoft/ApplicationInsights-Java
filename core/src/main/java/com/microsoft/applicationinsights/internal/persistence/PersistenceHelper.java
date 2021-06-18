package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;

final class PersistenceHelper {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceHelper.class);

    private static final long MAX_FILE_SIZE_IN_BYTES = 52428800; // 50MB
    private static final String PREFIX = "transmission";
    static final String PERMANENT_FILE_EXTENSION = ".trn";
    static final String TEMPORARY_FILE_EXTENSION = ".tmp";

    /**
     * Windows: C:\Users\{USER_NAME}\AppData\Local\Temp\applicationinsights
     * Linux: /var/temp/applicationinsights
     */
    static final File DEFAULT_FOlDER = new File(LocalFileSystemUtils.getTempDir(), "applicationinsights");

    static File createTempFileWithUniqueName() {
        File file = null;
        try {
            String prefix = PREFIX + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replaceAll("-", "");
            file = File.createTempFile(prefix, null, DEFAULT_FOlDER);
        } catch (IOException ex) {
            logger.error("Fail to create a temp file.", ex);
            // TODO (heya) track number of failures to create a temp file via Statsbeat
        }

        return file;
    }

    /**
     * Rename the given file's file extension.
     */
    static File renameFileExtension(String filename, String fileExtension) {
        File sourceFile = new File(DEFAULT_FOlDER, filename);
        File tempFile = new File(DEFAULT_FOlDER, FilenameUtils.getBaseName(filename) + fileExtension);
        try {
            FileUtils.moveFile(sourceFile, tempFile);
        } catch (IOException ex) {
            logger.error("Fail to change {} to have {} extension.", filename, fileExtension, ex);
            // TODO (heya) track number of failures to rename a file via Statsbeat
            return null;
        }

        return tempFile;
    }

    /**
     * Before a list of {@link ByteBuffer} can be persisted to disk, need to make sure capacity has not been reached yet.
     */
    static boolean maxFileSizeExceeded() {
        long size = getTotalSizeOfPersistedFiles();
        if (size >= MAX_FILE_SIZE_IN_BYTES) {
            logger.warn("Local persistent storage capacity has been reached. It's currently at {} KB. Telemetry will be lost.", (size/1024));
            return false;
        }

        return true;
    }

    private static long getTotalSizeOfPersistedFiles() {
        if (!DEFAULT_FOlDER.exists()) {
            return 0;
        }

        long sum = 0;
        Collection<File> files = FileUtils.listFiles(DEFAULT_FOlDER, new String[]{PERMANENT_FILE_EXTENSION}, false);
        for (File file : files) {
            sum += file.length();
        }

        return sum;
    }


    private PersistenceHelper() {}
}
