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
