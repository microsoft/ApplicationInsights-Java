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

package com.microsoft.applicationinsights.internal.logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The class is responsible for write log messages into log files.
 * The class will write to a file until the file max size is reached.
 *
 * When that happens it will create close that file and open a new file.
 *
 * When the maximum amount of log files is reached, the oldest one will be deleted.
 *
 * The class does a 'best effort' to work with files, if there is a problem, the class will
 * try to write into a {@link ConsoleLoggerOutput}
 * but will not fail the process under any scenario.
 */
public final class FileLoggerOutput implements LoggerOutput {
    private final static int MIN_SIZE_PER_LOG_FILE_IN_MB = 5;
    private final static int MAX_SIZE_PER_LOG_FILE_IN_MB = 500;
    private final static int MIN_NUMBER_OF_LOG_FILES = 2;
    private static String SDK_LOGS_DEFAULT_FOLDER = "javasdklogs";
    private static String SDK_LOGS_BASE_FOLDER_PATH = LocalFileSystemUtils.getTempDir().getAbsolutePath();
    private final static String LOG_FILE_SUFFIX_FOR_LISTING = "jsl";
    private final static String NUMBER_OF_FILES_ATTRIBUTE = "NumberOfFiles";
    private final static String TOTAL_SIZE_OF_LOG_FILES_IN_MB_ATTRIBUTE = "NumberOfTotalSizeInMB";
    private final static String LOG_FILES_BASE_FOLDER_PATH_ATTRIBUTE = "BaseFolderPath";
    private final static String UNIQUE_LOG_FILE_PREFIX_ATTRIBUTE = "UniquePrefix";
    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd-HH-mm-ss";

    private static class FileAndDate {
        public final File file;
        public final Date date;

        private FileAndDate(File file, Date date) {
            this.file = file;
            this.date = date;
        }
    }

    private String uniquePrefix;
    private LogFileProxy[] files;
    private int maxSizePerFileInMB;
    private int currentLogFileIndex;
    private File baseFolder;
    private LogFileProxyFactory factory;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

    private ConsoleLoggerOutput fallbackLoggerOutput = new ConsoleLoggerOutput();

    public FileLoggerOutput(Map<String, String> loggerData) {
        uniquePrefix = loggerData.get(UNIQUE_LOG_FILE_PREFIX_ATTRIBUTE);
        if (StringUtils.isEmpty(uniquePrefix)) {
            throw new IllegalArgumentException(String.format("Unique log file prefix is not defined"));
        }

        uniquePrefix += '-';

        int numberOfFiles = getRequest(loggerData, NUMBER_OF_FILES_ATTRIBUTE, MIN_NUMBER_OF_LOG_FILES);
        int numberOfTotalMB = getRequest(loggerData, TOTAL_SIZE_OF_LOG_FILES_IN_MB_ATTRIBUTE, MIN_SIZE_PER_LOG_FILE_IN_MB);

        String baseFolderPath = loggerData.get(LOG_FILES_BASE_FOLDER_PATH_ATTRIBUTE);

        factory = new DefaultLogFileProxyFactory();

        initialize(baseFolderPath, numberOfFiles, numberOfTotalMB);
    }

    private int getRequest(Map<String, String> loggerData, String requestName, int defaultValue) {
        int requestValue = defaultValue;
        String requestValueAsString = loggerData.get(requestName);
        if (StringUtils.isNotEmpty(requestValueAsString)) {
            try {
                requestValue = Integer.valueOf(loggerData.get(requestName));
            } catch (Exception e) {
                fallbackLoggerOutput.log(String.format("Error: invalid value '%s' for '%s', using default: %d", requestValueAsString, requestName, defaultValue));
            }
        }

        return requestValue;
    }

    private void initialize(String baseFolderPath, int numberOfFiles, int numberOfTotalMB) {
        currentLogFileIndex = 0;
        Path logFilePath;

        if (StringUtils.isEmpty(baseFolderPath)) {
            baseFolderPath = SDK_LOGS_BASE_FOLDER_PATH;

            // If no path is specified by user create log file directory in temp with default folder
            // name.
            logFilePath = Paths.get(baseFolderPath, SDK_LOGS_DEFAULT_FOLDER);
        }

        else {
            // Use the user-specified absolute file path for logging.
            logFilePath = Paths.get(baseFolderPath);
        }

        if (numberOfFiles < MIN_NUMBER_OF_LOG_FILES) {
            numberOfFiles = MIN_NUMBER_OF_LOG_FILES;
        }

        files = new LogFileProxy[numberOfFiles];

        int tempSizePerFileInMB = numberOfTotalMB / numberOfFiles;
        if (tempSizePerFileInMB < MIN_SIZE_PER_LOG_FILE_IN_MB) {
            tempSizePerFileInMB = MIN_SIZE_PER_LOG_FILE_IN_MB;
        } else if (tempSizePerFileInMB > MAX_SIZE_PER_LOG_FILE_IN_MB) {
            tempSizePerFileInMB = MAX_SIZE_PER_LOG_FILE_IN_MB;
        }
        this.maxSizePerFileInMB = tempSizePerFileInMB;

        if (!Files.exists(logFilePath)) {
            try {
                baseFolder = Files.createDirectories(logFilePath).toFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            baseFolder = logFilePath.toFile();
            attachToExisting();
        }

    }

    @Override
    public synchronized void log(String message) {
        try {
            LogFileProxy logFileProxy = getCurrentLogFileProxy();
            if (logFileProxy != null) {
                logFileProxy.writeLine(message);
            }
        } catch (IOException e) {
            fallbackLoggerOutput.log(String.format("Failed to write to log to file exception: %s. Message '%s'", e.toString(), message));
        }
    }

    /**
     * After this method is called the instance should not be called again for logging messages
     */
    @Override
    public void close() {
        LogFileProxy currentLogger = files[currentLogFileIndex];
        if (currentLogger != null) {
            try {
                files[currentLogFileIndex] = null;
                currentLogger.close();
            } catch (IOException e) {
            }
        }
    }

    public void setLogProxyFactory(LogFileProxyFactory factory) {
        this.factory = factory;
    }

    private LogFileProxy getCurrentLogFileProxy() throws IOException {
        LogFileProxy currentProxy = files[currentLogFileIndex];
        if (currentProxy != null && !currentProxy.isFull()) {
            return currentProxy;
        }

        return createNewFileProxy();
    }

    private LogFileProxy createNewFileProxy() throws IOException {
        LogFileProxy currentLogger = files[currentLogFileIndex];
        if (currentLogger != null) {
            try {
                currentLogger.close();
            } catch (IOException e) {
                // Failed to close but that should not stop us
                fallbackLoggerOutput.log(String.format("Failed to close log file, exception: %s", e.toString()));
            }
        }

        ++currentLogFileIndex;
        if (currentLogFileIndex == files.length) {
            currentLogFileIndex = 0;
        }

        currentLogger = files[currentLogFileIndex];
        if (currentLogger != null) {
            files[currentLogFileIndex] = null;

            // Best effort
            try {
                currentLogger.delete();
            } catch (Exception e) {
                // Failed to delete but that should not stop us
                fallbackLoggerOutput.log(String.format("Failed to delete log file, exception: %s", e.toString()));
            }
        }

        Calendar cal = Calendar.getInstance();
        String filePrefix = uniquePrefix + simpleDateFormat.format(cal.getTime());
        LogFileProxy logFileProxy = factory.create(baseFolder, filePrefix, maxSizePerFileInMB);
        files[currentLogFileIndex] = logFileProxy;


        return logFileProxy;
    }

    private void attachToExisting() {
        try {
            List<FileAndDate> oldLogs = getExistingLogsFromNewToOld();

            attachToExisting(oldLogs);
        } catch (Exception e) {
            // Failed to delete but that should not stop us
            fallbackLoggerOutput.log(String.format("Failed to delete old log file, exception: %s", e.toString()));
        }
    }

    private void attachToExisting(List<FileAndDate> oldLogs) {
        if (oldLogs.isEmpty()) {
            return;
        }

        int filesIndex = currentLogFileIndex;
        int numberOfFilesFound = 0;
        for (FileAndDate oldLog : oldLogs) {
            try {
                if (numberOfFilesFound < files.length) {
                    LogFileProxy logFileProxy = factory.attach(oldLog.file, maxSizePerFileInMB);
                    if (logFileProxy == null) {
                        continue;
                    }

                    ++numberOfFilesFound;
                    files[filesIndex] = logFileProxy;
                    ++filesIndex;
                } else {
                    oldLog.file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<FileAndDate> getExistingLogsFromNewToOld() {
        try {
            Collection<File> oldLogs = FileUtils.listFiles(baseFolder, new String[]{LOG_FILE_SUFFIX_FOR_LISTING}, false);
            List<File> asList;
            if (!(oldLogs instanceof List)) {
                asList = new ArrayList<>(oldLogs);
            } else {
                asList = (List<File>)oldLogs;
            }

            ArrayList<FileAndDate> filesByDate = new ArrayList<FileAndDate>();
            for (File file : asList) {
                Date fileDate = getFileDate(file);
                if (fileDate == null) {
                    continue;
                }

                filesByDate.add(new FileAndDate(file, fileDate));
            }

            Collections.sort(filesByDate, new Comparator<FileAndDate>() {
                @Override
                public int compare(FileAndDate file1, FileAndDate file2) {
                    if (file1.date.before(file2.date)) {
                        return 1;
                    } else if (file2.date.before(file1.date)) {
                        return -1;
                    }

                    return 0;
                }
            });

            return filesByDate;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Date getFileDate(File file) {
        try {
            String fileName = FilenameUtils.getBaseName(file.getName());
            int index = fileName.indexOf(uniquePrefix);
            if (index != -1) {
                String dateString = fileName.substring(index + uniquePrefix.length(), index + uniquePrefix.length() + DATE_FORMAT_NOW.length());
                Date date = simpleDateFormat.parse(dateString);

                return date;
            }
        } catch (Exception e) {
        }

        return null;
    }
}
