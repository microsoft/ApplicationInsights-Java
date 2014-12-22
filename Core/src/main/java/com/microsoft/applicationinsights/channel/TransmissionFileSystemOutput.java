package com.microsoft.applicationinsights.channel;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The class knows how to manage {@link com.microsoft.applicationinsights.channel.Transmission} that needs
 * to be saved to the file system.
 *
 * The class works on a pre-defined folder and should know the size of disk it can use.
 *
 * With that data it knows how to store incoming Transmissions and store them into files that can be later
 * be read back into Transmissions.
 *
 * Created by gupele on 12/18/2014.
 */
public class TransmissionFileSystemOutput implements TransmissionOutput {
    private final static String TRANSMISSION_FILE_PREFIX = "Transmission";
    private final static String TRANSMISSION_DEFAULT_FOLDER = "transmissions";
    private final static String TRANSMISSION_FILE_EXTENSION = ".trn";
    private final static String TRANSMISSION_FILE_EXTENSION_FOR_SEARCH = "trn";

    private final static int DEFAULT_CAPACITY_KILOBYTES = 10 * 1024;


    /// The folder in which we save transmission files
    private File folder;

    /// Capacity is the size of disk that we are can use
    private long capacity = DEFAULT_CAPACITY_KILOBYTES * 1024;

    /// The size of the current files we have on the disk
    private final AtomicLong size;

    public TransmissionFileSystemOutput() {
        this(System.getProperty("java.io.tmpdir") + File.separator + TRANSMISSION_DEFAULT_FOLDER);
    }

    public TransmissionFileSystemOutput(String folderPath) {
        Preconditions.checkNotNull(folderPath, "folderPath must be a non-null value");

        folder = new File(folderPath);
        if (!folder.exists() || !folder.canRead() || !folder.canWrite()) {
            throw new IllegalArgumentException("Folder must exist with read and write permissions");
        }

        long currentSize = getTotalSizeOfTransmissionFiles();
        size = new AtomicLong(currentSize);
    }

    @Override
    public boolean send(Transmission transmission) {
        if (size.get() >= capacity) {
            return false;
        }

        Optional<File> tempTransmissionFile = createTemporaryFile();
        if (!tempTransmissionFile.isPresent()) {
            return false;
        }

        if (!saveTransmission(tempTransmissionFile.get(), transmission)) {
            return false;
        }

        if (!renameToPermanentName(tempTransmissionFile.get())) {
            return false;
        }

        return true;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
    }

    public void setCapacity(long capacity) {
        Preconditions.checkArgument(capacity > 0, "capacity should be a positive number");

        this.capacity = capacity;
    }

    private boolean renameToPermanentName(File tempTransmissionFile) {
        File transmissionFile = new File(folder, FilenameUtils.getBaseName(tempTransmissionFile.getName()) + TRANSMISSION_FILE_EXTENSION);
        if (tempTransmissionFile.renameTo(transmissionFile)) {
            size.addAndGet(transmissionFile.length());
            return true;
        }

        return false;
    }

    private boolean saveTransmission(File transmissionFile, Transmission transmission) {
        try {
            FileUtils.writeByteArrayToFile(transmissionFile, transmission.getContent());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Optional<File> createTemporaryFile() {
        File file = null;
        try {
            file = File.createTempFile(TRANSMISSION_FILE_PREFIX, null, folder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.fromNullable(file);
    }

    private long getTotalSizeOfTransmissionFiles() {
        Collection<File> transmissions = FileUtils.listFiles(folder, new String[] {TRANSMISSION_FILE_EXTENSION_FOR_SEARCH}, false);

        long totalSize = 0;
        for (File file : transmissions) {
            totalSize += file.length();
        }

        return totalSize;
    }
}
