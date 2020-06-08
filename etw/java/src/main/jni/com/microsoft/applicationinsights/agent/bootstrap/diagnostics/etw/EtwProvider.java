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
package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw;

import java.io.File;
import java.io.IOException;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventBase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtwProvider {
    private static final String LIB_FILENAME_32_BIT = "applicationinsights-java-etw-provider-x86.dll";
    private static final String LIB_FILENAME_64_BIT = "applicationinsights-java-etw-provider-x86-64.dll";

    // TODO make sure this logger does not append to EtwAppender
    private static Logger LOGGER;

    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            LOGGER = LoggerFactory.getLogger(EtwProvider.class);
            File dllPath = null;
            try {
                dllPath = loadLibrary();
                LOGGER.info("EtwProvider initialized. Lib path={}", dllPath.getAbsolutePath());
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                try {
                    LOGGER.error("Error initializing EtwProvider", t);
                    if (dllPath != null) {
                        dllPath.deleteOnExit();
                    }
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable chomp) {
                    // ignore
                }
            }
        } else {
            LoggerFactory.getLogger(EtwProvider.class).info("Non-Windows OS. Loading ETW library skipped.");
        }
    }

    private static File loadLibrary() throws IOException {
        final String fileName = getDllFilenameForArch();

        final File targetDir = DllFileUtils.buildDllLocalPath();
        final File dllPath = new File(targetDir, fileName);

        if (!dllPath.exists()) {
            DllFileUtils.extractToLocalFolder(dllPath, fileName);
        }

        System.load(dllPath.getAbsolutePath());

        return dllPath;
    }

    static String getDllFilenameForArch() {
        final boolean is32bit = StringUtils.defaultIfEmpty(System.getProperty("os.arch"), "null").equalsIgnoreCase("x86");
        return is32bit ? LIB_FILENAME_32_BIT : LIB_FILENAME_64_BIT;
    }

    private native void cppWriteEvent(IpaEtwEventBase event) throws ApplicationInsightsEtwException;

    public void writeEvent(IpaEtwEventBase event) throws ApplicationInsightsEtwException {
        cppWriteEvent(event);
    }
}