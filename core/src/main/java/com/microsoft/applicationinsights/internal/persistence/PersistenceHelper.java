package com.microsoft.applicationinsights.internal.persistence;

import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import org.apache.commons.codec.digest.DigestUtils;
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
    static final String PERMANENT_FILE_EXTENSION = ".trn";
    static final String TEMPORARY_FILE_EXTENSION = ".tmp";

    /**
     * Windows: C:\Users\{USER_NAME}\AppData\Local\Temp\applicationinsights
     * Linux: /var/temp/applicationinsights
     */
    static final File DEFAULT_ROOT_FOLDER = new File(LocalFileSystemUtils.getTempDir(), "applicationinsights");

    static File createTempFileWithUniqueName() {
        File file = null;
        try {
            String prefix = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replaceAll("-", "");
            file = File.createTempFile(prefix, null, getDefaultSubdirectory());
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
        File sourceFile = new File(getDefaultSubdirectory(), filename);
        File tempFile = new File(getDefaultSubdirectory(), FilenameUtils.getBaseName(filename) + fileExtension);
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

    static File getDefaultSubdirectory() {
        String subdirectoryHash = getCurrentProcessSubdirectoryHash();
        File subdirectory = new File(DEFAULT_ROOT_FOLDER, subdirectoryHash);
        if (!subdirectory.exists()) {
            subdirectory.mkdir();
        }

        if (!subdirectory.exists() || !subdirectory.canRead() || !subdirectory.canWrite()) {
            throw new IllegalArgumentException("subdirectory must exist and have read and write permissions.");
        }

        return subdirectory;
    }

    private static String getCurrentProcessSubdirectoryHash() {
        String appIdentifier = System.getProperty("user.name") + "@" + SystemInformation.INSTANCE.getProcessId();
        String hash = null;
        try {
            hash = DigestUtils.sha256Hex(appIdentifier);
        } catch (Exception ex) {
            logger.error("Fail to generate a sha 256 hash for the current process id.", ex);
        }

        return hash;
    }

    private static long getTotalSizeOfPersistedFiles() {
        if (!DEFAULT_ROOT_FOLDER.exists()) {
            return 0;
        }

        long sum = 0;
        Collection<File> files = FileUtils.listFiles(getDefaultSubdirectory(), new String[]{PERMANENT_FILE_EXTENSION}, false);
        for (File file : files) {
            sum += file.length();
        }

        return sum;
    }

    private PersistenceHelper() {}
}
