/*
 * AppInsights-Java
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Strings;

/**
 * The class is responsible for write log messages into log files.
 * The class will write to a file until the file max size is reached.
 *
 * When that happens it will create close that file and open a new file.
 *
 * When the maximum amount of log files is reached, the oldest one will be deleted.
 *
 * The class does a 'best effort' to work with files, if there is a problem, the class will
 * try to write into a {@link com.microsoft.applicationinsights.internal.logger.ConsoleLoggerOutput}
 * but will not fail the process under any scenario.
 */
public final class FileLoggerOutput implements LoggerOutput {
    private final static int MIN_SIZE_PER_LOG_FILE_IN_MB = 5;
    private final static int MAX_SIZE_PER_LOG_FILE_IN_MB = 500;
    private final static int MIN_NUMBER_OF_LOG_FILES = 2;
    private static String SDK_LOGS_DEFAULT_FOLDER = "javasdklogs";
    private final static String LOG_FILE_SUFFIX_FOR_LISTING = "jsl";

    private LogFileProxy[] files;
    private int maxSizePerFileInMB;
    private int currentLogFileIndex;
    private File baseFolder;
    private LogFileProxyFactory factory;

    private ConsoleLoggerOutput alternativeLoggerOutput = new ConsoleLoggerOutput();

    public FileLoggerOutput(Map<String, String> loggerData) {
        int numberOfFiles = 0;
        try {
            numberOfFiles = Integer.valueOf(loggerData.get("NumberOfFiles"));
        } catch (Exception e) {
        }
        int numberOfTotalMB = 0;
        try {
            numberOfTotalMB = Integer.valueOf(loggerData.get("NumberOfTotalSizeInMB"));
        } catch (Exception e) {
        }
        String baseFolderName = loggerData.get("BaseFolder");
        initialize(baseFolderName, numberOfFiles, numberOfTotalMB);

        factory = new DefaultLogFileProxyFactory();
    }

    private void initialize(String baseFolderName, int numberOfFiles, int numberOfTotalMB) {
        if (Strings.isNullOrEmpty(baseFolderName)) {
            baseFolderName = SDK_LOGS_DEFAULT_FOLDER;
        }

        baseFolder = new File(System.getProperty("java.io.tmpdir"), baseFolderName);
        if (!baseFolder.exists()) {
            baseFolder.mkdir();
        }

        cleanOld();

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

        this.currentLogFileIndex = 0;
    }

    @Override
    public synchronized void log(String message) {
        try {
            LogFileProxy logFileProxy = prepareLogFileProxy();
            if (logFileProxy != null) {
                logFileProxy.writeLine(message);
            }
        } catch (IOException e) {
            alternativeLoggerOutput.log(String.format("Failed to write to log to file exception: %s. Message '%s'", e.getMessage(), message));
        }
    }

    @Override
    public void close() {
        LogFileProxy currentLogger = files[currentLogFileIndex];
        if (currentLogger != null) {
            try {
                currentLogger.close();
            } catch (IOException e) {
            }
        }
    }

    void setLogProxyFactory(LogFileProxyFactory factory) {
        this.factory = factory;
    }

    private LogFileProxy prepareLogFileProxy() throws IOException {
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
                alternativeLoggerOutput.log(String.format("Failed to close log file, exception: %s", e.getMessage()));
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
                alternativeLoggerOutput.log(String.format("Failed to delete log file, exception: %s", e.getMessage()));
            }
        }

        files[currentLogFileIndex] = factory.create(baseFolder, maxSizePerFileInMB);
        return files[currentLogFileIndex];
    }

    private void cleanOld() {
        try {
            Collection<File> oldLogs = FileUtils.listFiles(baseFolder, new String[]{LOG_FILE_SUFFIX_FOR_LISTING}, false);
            for (File oldLog : oldLogs) {
                oldLog.delete();
            }
        } catch (Exception e) {
            // Failed to delete but that should not stop us
            alternativeLoggerOutput.log(String.format("Failed to delete old log file, exception: %s", e.getMessage()));
        }
    }
}
