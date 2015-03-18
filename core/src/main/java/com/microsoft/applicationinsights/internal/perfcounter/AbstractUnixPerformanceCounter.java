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

package com.microsoft.applicationinsights.internal.perfcounter;

import java.io.File;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A base class for Unix performance counters who uses the '/proc/' filesystem for their work.
 *
 * Created by gupele on 3/8/2015.
 */
abstract class AbstractUnixPerformanceCounter extends AbstractPerformanceCounter {
    private final File processFile;
    private final String path;

    protected AbstractUnixPerformanceCounter(String path) {
        Preconditions.checkState(SystemInformation.INSTANCE.isUnix(), "This performance counter must be activated in Unix environment.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "path should be non null, non empty value.");

        this.path = path;
        processFile = new File(path);
        if (!processFile.canRead()) {
            logError("Can not read");
        }
    }

    protected void logError(String format, Object... args) {
        format = "Performance Counter " + getId() + ": Error in file '" + path + "': " + format;
        InternalLogger.INSTANCE.error(format, args);
    }

    protected File getProcessFile() {
        return processFile;
    }
}
