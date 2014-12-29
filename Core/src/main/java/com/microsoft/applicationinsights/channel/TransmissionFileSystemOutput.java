package com.microsoft.applicationinsights.channel;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Comparator;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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
    private final static String TEMP_FILE_EXTENSION = ".tmp";
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
        this(new File(System.getProperty("java.io.tmpdir"), TRANSMISSION_DEFAULT_FOLDER).getPath());
    }

    public TransmissionFileSystemOutput(String folderPath) {
        Preconditions.checkNotNull(folderPath, "folderPath must be a non-null value");

        folder = new File(folderPath);

        if (!folder.exists()) {
            folder.mkdir();
        }

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

    public Transmission fetchOldestFile() {
        Collection<Transmission> files = fetchOldestFiles(1);
        if (files.isEmpty()) {
            return null;
        }

        return files.iterator().next();
    }

    public synchronized Collection<Transmission> fetchOldestFiles(int limit) {
        try {
            Collection<File> transmissions = FileUtils.listFiles(folder, new String[] {TRANSMISSION_FILE_EXTENSION_FOR_SEARCH}, false);
            if (transmissions.isEmpty()) {
                return Collections.emptyList();
            }

            List<File> filesToLoad = sortAndTrim(transmissions, limit);

            ArrayList<Transmission> loadedTransmissions = new ArrayList<Transmission>(filesToLoad.size());

            for (File file : filesToLoad) {
                Optional<File> fileOptional = renameToTemporaryName(file);
                if (!fileOptional.isPresent()) {
                    continue;
                }

                File tempFile = fileOptional.get();
                Optional<Transmission> transmission = loadTransmission(tempFile);

                tempFile.delete();

                if (transmission.isPresent()) {
                    loadedTransmissions.add(transmission.get());
                }
            }

            return loadedTransmissions;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public void setCapacity(long capacity) {
        Preconditions.checkArgument(capacity > 0, "capacity should be a positive number");

        this.capacity = capacity;
    }

    private List<File> sortAndTrim(Collection<File> transmissions, int limit) {
        List<File> asList;
        if (!(transmissions instanceof List)) {
            asList = Lists.newArrayList(transmissions);
        } else {
            asList = (List<File>)transmissions;
        }

        Collections.sort(asList, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long file1LastModified = file1.lastModified();
                long file2LastModified = file2.lastModified();
                if (file1LastModified < file2LastModified) {
                    return 1;
                } else if (file1LastModified > file2LastModified) {
                    return -1;
                }

                return 0;
            }
        });

        if (asList.size() > limit) {
            asList = asList.subList(0, limit);
        }

        return asList;
    }

    private Optional<Transmission> loadTransmission(File file) {
        Transmission transmission = null;

        InputStream fileInput = null;
        ObjectInput input = null;
        try {
            if (file == null) {
                return Optional.absent();
            }

            fileInput = new FileInputStream(file);
            InputStream buffer = new BufferedInputStream(fileInput);
            input = new ObjectInputStream (buffer);
            transmission = (Transmission)input.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return Optional.fromNullable(transmission);
    }

    private boolean renameToPermanentName(File tempTransmissionFile) {
        File transmissionFile = new File(folder, FilenameUtils.getBaseName(tempTransmissionFile.getName()) + TRANSMISSION_FILE_EXTENSION);
        if (tempTransmissionFile.renameTo(transmissionFile)) {
            size.addAndGet(transmissionFile.length());
            return true;
        }

        return false;
    }

    private Optional<File> renameToTemporaryName(File tempTransmissionFile) {
        File transmissionFile = null;
        try {
            File renamedFile = new File(folder, FilenameUtils.getBaseName(tempTransmissionFile.getName()) + TEMP_FILE_EXTENSION);
            if (tempTransmissionFile.renameTo(renamedFile)) {
                size.addAndGet(-renamedFile.length());
                transmissionFile = renamedFile;
            }
        } catch (Exception ignore) {
            // Consume the exception, since there isn't anything 'smart' to do now
        }

        return Optional.fromNullable(transmissionFile);
    }

    private boolean saveTransmission(File transmissionFile, Transmission transmission) {
        try {
            OutputStream fileOutput = new FileOutputStream(transmissionFile);
            OutputStream buffer = new BufferedOutputStream(fileOutput);
            ObjectOutput output = new ObjectOutputStream(buffer);
            try{
                output.writeObject(transmission);
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                try {
                    output.close();
                } catch (Exception e) {
                    return false;
                }
            }
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
