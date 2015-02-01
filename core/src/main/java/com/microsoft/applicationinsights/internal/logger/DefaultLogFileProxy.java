package com.microsoft.applicationinsights.internal.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The class is responsible for doing the work with the log files.
 */
public final class DefaultLogFileProxy implements LogFileProxy {
    private static String NEW_LINE = System.getProperty("line.separator");
    private final static String LOG_FILE_PREFIX = "JavaSDKLog";
    private final static String LOG_FILE_SUFFIX = ".jsl";

    private FileWriter out;
    private File file;
    private int maxSizePerFileInMB;

    public DefaultLogFileProxy(File baseFolder, int maxSizePerFileInMB) throws IOException {
        this.maxSizePerFileInMB = maxSizePerFileInMB;
        file = File.createTempFile(LOG_FILE_PREFIX, LOG_FILE_SUFFIX, baseFolder);
        out = new FileWriter(file);
    }

    public void close() throws IOException {
        flush();
        out.close();
    }

    public void delete() {
        file.delete();
    }

    public void writeLine(String line) throws IOException {
        out.write(line + NEW_LINE);
    }

    public boolean isFull() {
        long fileSizeInMB = file.length() / 1000000;
        return maxSizePerFileInMB < fileSizeInMB;
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
        }
    }
}
