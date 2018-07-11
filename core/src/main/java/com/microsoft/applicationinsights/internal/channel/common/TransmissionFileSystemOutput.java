/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.channel.common;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * The class knows how to manage {@link Transmission} that needs
 * to be saved to the file system.
 *
 * The class works on a pre-defined folder and should know the size of disk it can use.
 *
 * With that data it knows how to store incoming Transmissions and store them into files that can be later
 * be read back into Transmissions.
 *
 * Created by gupele on 12/18/2014.
 */
public final class TransmissionFileSystemOutput implements TransmissionOutput {
    private final static String TRANSMISSION_FILE_PREFIX = "Transmission";
    private final static String TRANSMISSION_DEFAULT_FOLDER = "transmissions";
    private final static String TEMP_FILE_EXTENSION = ".tmp";
    private final static String TRANSMISSION_FILE_EXTENSION = ".trn";
    private final static String TRANSMISSION_FILE_EXTENSION_FOR_SEARCH = "trn";
    private final static int NUMBER_OF_FILES_TO_CACHE = 128;

    private final static int MAX_RETRY_FOR_DELETE = 2;
    private final static int DELETE_TIMEOUT_ON_FAILURE_IN_MILLS = 100;

    public final static int DEFAULT_CAPACITY_MEGABYTES = 10;
    private final static int MAX_CAPACITY_MEGABYTES = 1000;
    private final static int MIN_CAPACITY_MEGABYTES = 1;
    private static final String MAX_TRANSMISSION_STORAGE_CAPACITY_NAME = "Channel.MaxTransmissionStorageCapacityInMB";


    /// The folder in which we save transmission files
    private File folder;

    /// Capacity is the size of disk that we are can use
    private long capacityInKB = DEFAULT_CAPACITY_MEGABYTES * 1024;

    LimitsEnforcer capacityEnforcer;

    /// The size of the current files we have on the disk
    private final AtomicLong size;

    /// Cache old files here to re-send to have better performance
    private final ArrayList<File> cacheOfOldestFiles = new ArrayList<File>();
    private final HashSet<String> filesThatAreBeingLoaded = new HashSet<String>();

    public TransmissionFileSystemOutput(String folderPath, String maxTransmissionStorageCapacity) {
        if (folderPath == null) {
            folderPath = new File(LocalFileSystemUtils.getTempDir(), TRANSMISSION_DEFAULT_FOLDER).getPath();
        }

        capacityEnforcer = LimitsEnforcer.createWithClosestLimitOnError(MIN_CAPACITY_MEGABYTES,
                                                                        MAX_CAPACITY_MEGABYTES,
                                                                        DEFAULT_CAPACITY_MEGABYTES,
                                                                        MAX_TRANSMISSION_STORAGE_CAPACITY_NAME,
                                                                        maxTransmissionStorageCapacity);
        capacityInKB = capacityEnforcer.getCurrentValue() * 1024;

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

    public TransmissionFileSystemOutput() {
        this(null, null);
    }

    public TransmissionFileSystemOutput(String folderPath) {
        this(folderPath, null);
    }

    @Override
    public boolean send(Transmission transmission) {

        long currentSizeInKb = size.get();
        if (currentSizeInKb >= capacityInKB * 1024) {
        	InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.WARN, "Persistent storage max capacity has been reached; currently at %s KB. Telemetry will be lost, please set the MaxTransmissionStorageFilesCapacityInMB property in the configuration file.", currentSizeInKb);
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

        InternalLogger.INSTANCE.info("Data persisted to file. To be sent when the network is available.");
        return true;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
    }

    public Transmission fetchOldestFile() {
        try {
            Optional<File> oldestFile = fetchOldestFromCache();
            if (!oldestFile.isPresent()) {
                return null;
            }

            String fileName = oldestFile.get().getName();
            try {
                Optional<File> oldestFileAsTemp = renameToTemporaryName(oldestFile.get());
                if (!oldestFileAsTemp.isPresent()) {
                    return null;
                }

                File tempFile = oldestFileAsTemp.get();
                Optional<Transmission> transmission = loadTransmission(tempFile);

                // On the vast majority of times this should work
                // but there might be some timing issues, that's why we try twice
                for (int deleteCounter = 0; deleteCounter < MAX_RETRY_FOR_DELETE; ++deleteCounter) {
                    if (tempFile.delete()) {
                        break;
                    }

                    try {
                        Thread.sleep(DELETE_TIMEOUT_ON_FAILURE_IN_MILLS);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                return transmission.get();
            } finally {
                synchronized (this) {
                    filesThatAreBeingLoaded.remove(fileName);
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    public void setCapacity(int suggestedCapacity) {
        this.capacityInKB = capacityEnforcer.normalizeValue(suggestedCapacity) * 1024;
    }

    private List<File> sortOldestLastAndTrim(Collection<File> transmissions, int limit) {
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
            InternalLogger.INSTANCE.error("Failed to load transmission, file not found, exception: %s", e.toString());
        } catch (ClassNotFoundException e) {
            InternalLogger.INSTANCE.error("Failed to load transmission, non transmission, exception: %s", e.toString());
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to load transmission, io exception: %s", e.toString());
        } finally{
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }

        return Optional.fromNullable(transmission);
    }

    private boolean renameToPermanentName(File tempTransmissionFile) {
        File transmissionFile = new File(folder, FilenameUtils.getBaseName(tempTransmissionFile.getName()) + TRANSMISSION_FILE_EXTENSION);
        try {
            long fileLength = tempTransmissionFile.length();
            FileUtils.moveFile(tempTransmissionFile, transmissionFile);
            size.addAndGet(fileLength);
            return true;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Rename To Permanent Name failed, exception: %s", e.toString());
        }

        return false;
    }

    private Optional<File> renameToTemporaryName(File tempTransmissionFile) {
        File transmissionFile = null;
        try {
            File renamedFile = new File(folder, FilenameUtils.getBaseName(tempTransmissionFile.getName()) + TEMP_FILE_EXTENSION);
            FileUtils.moveFile(tempTransmissionFile, renamedFile);
            size.addAndGet(-renamedFile.length());
            transmissionFile = renamedFile;
        } catch (Exception ignore) {
            InternalLogger.INSTANCE.error("Rename To Temporary Name failed, exception: %s", ignore.toString());
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
                InternalLogger.INSTANCE.error("Failed to save transmission, exception: %s", e.toString());
            } finally{
                try {
                    output.close();
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to save transmission, exception: %s", e.toString());
        }

        return false;
    }

    private Optional<File> createTemporaryFile() {
        File file = null;
        try {
            file = File.createTempFile(TRANSMISSION_FILE_PREFIX, null, folder);
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to create temporary file, exception: %s", e.toString());
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

    private Optional<File> fetchOldestFromCache() {
        synchronized (this) {
            if (cacheOfOldestFiles.isEmpty()) {

                // Fill the cache
                Collection<File> transmissions = FileUtils.listFiles(folder, new String[] {TRANSMISSION_FILE_EXTENSION_FOR_SEARCH}, false);

                if (transmissions.isEmpty()) {
                    // No files
                    return Optional.absent();
                }

                List<File> filesToLoad = sortOldestLastAndTrim(transmissions, NUMBER_OF_FILES_TO_CACHE);

                if (filesToLoad == null || filesToLoad.isEmpty()) {
                    return Optional.absent();
                }

                cacheOfOldestFiles.addAll(filesToLoad);
            }

            File fileToLoad = cacheOfOldestFiles.remove(cacheOfOldestFiles.size() - 1);

            String fileName = fileToLoad.getName();
            if (filesThatAreBeingLoaded.contains(fileName)) {
                return Optional.absent();
            }

            filesThatAreBeingLoaded.add(fileName);
            // Remove oldest which is the last one, this is optimized for not doing a copy
            return Optional.fromNullable(fileToLoad);
        }
    }
}
