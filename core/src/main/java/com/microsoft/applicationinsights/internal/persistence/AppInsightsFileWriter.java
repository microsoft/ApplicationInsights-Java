package com.microsoft.applicationinsights.internal.persistence;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.applicationinsights.internal.persistence.AppInsightsFileLoader.DEFAULT_FOlDER;

/**
 * This class manages writing a list of {@link ByteBuffer} to the file system.
 */
public final class AppInsightsFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(AppInsightsFileWriter.class);

    // track the size of the file
    private final AtomicLong size = new AtomicLong();

    public AppInsightsFileWriter() {
        if (!DEFAULT_FOlDER.exists()) {
            DEFAULT_FOlDER.mkdir();
        }

        if (!DEFAULT_FOlDER.exists() || !DEFAULT_FOlDER.canRead() || !DEFAULT_FOlDER.canWrite()) {
            throw new IllegalArgumentException("{} must exist with read and write permissions.");
        }
    }

    public boolean writeToDisk(List<ByteBuffer> byteBuffers) {
        AtomicReference<File> file = createTemporaryFile();
        if (file == null) {
            return false;
        }

        if (!saveByteBuffers(file.get(), byteBuffers)) {
            return false;
        }

        if (!renameToPermanentName(file.get())) {
            return false;
        }

        logger.info("List<ByteBuffers> has been persisted to file and will be sent when the network becomes available.");
        // TODO (heya) track data persistence success via Statsbeat
        return true;
    }

    private AtomicReference<File> createTemporaryFile() {
        AtomicReference<File> file = new AtomicReference<>();
        try {
            String prefix = "bytebuffers" + "-" + System.currentTimeMillis() + "-";
            file.set(File.createTempFile(prefix, null, DEFAULT_FOlDER));
        } catch (IOException ex) {
            // TODO (heya) track number of failures to create a temp file via Statsbeat
            logger.error("Fail to create a temporary file for disk persistence. {}", ex.getCause());
        }

        return file;
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

//        List<byte[]> byteArray = byteBuffers.stream().map(ByteBuffer::array).collect(Collectors.toList());
//        try (ObjectOutput out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
//            out.writeObject(byteArray);
//        } catch (IOException ex) {
//            // TODO (heya) track IO write failure via Statsbeat
//            logger.error("Fail to write to file.", ex);
//            return false;
//        }
//
//        return true;
    }

    private boolean renameToPermanentName(File tempFile) {
        File file = new File(DEFAULT_FOlDER, FilenameUtils.getBaseName(tempFile.getName()) + ".trn");
        try {
            FileUtils.moveFile(tempFile, file);
            size.addAndGet(tempFile.length());
            AppInsightsFileLoader.get().addPersistedFilenameToMap(file.getName());
            return true;
        } catch (IOException ex) {
            // TODO (heya) track renaming failure via Statsbeat
            logger.error("Fail to rename file to a permanent name.", ex);
        }

        return false;
    }
}
